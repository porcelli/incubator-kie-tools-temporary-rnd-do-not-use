/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.marshalling.rebind;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.common.rebind.EnvironmentUtil;
import org.jboss.errai.marshalling.server.util.ServerMarshallUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class MarshallersGenerator extends Generator {

  private static final Logger logger = LoggerFactory.getLogger(Generator.class);

  public static final String SERVER_MARSHALLER_PACKAGE_NAME = "org.jboss.errai.marshalling.server.impl";
  public static final String SERVER_MARSHALLER_CLASS_NAME = "ServerMarshallingFactoryImpl";
  private static final String SERVER_MARSHALLER_OUTPUT_DIR_PROP = "errai.marshalling.server.classOutput";
  private static final String SERVER_MARSHALLER_OUTPUT_ENABLED_PROP = "errai.marshalling.server.classOutput.enabled";

  private static final String SERVER_MARSHALLER_OUTPUT_DIR =
          System.getProperty(SERVER_MARSHALLER_OUTPUT_DIR_PROP) != null ?
                  System.getProperty(SERVER_MARSHALLER_OUTPUT_DIR_PROP) :
                  null;

  private static final boolean SERVER_MARSHALLER_OUTPUT_ENABLED =
          Boolean.valueOf(System.getProperty(SERVER_MARSHALLER_OUTPUT_ENABLED_PROP, "true"));

  private static final String[] candidateOutputDirectories =
          {"target/classes/", "war/WEB-INF/classes/", "web/WEB-INF/classes/", "target/war/WEB-INF/classes/",
                  "WEB-INF/classes/", "src/main/webapp/WEB-INF/classes/"};

  private static final DiscoveryStrategy[] rootDiscoveryStrategies;

  static {
    rootDiscoveryStrategies = new DiscoveryStrategy[]{
            new DiscoveryStrategy() {
              @Override
              public String getCandidate(GeneratorContext context, DiscoveryVeto veto) {
                try {
                  ConfigurationProperty prop = context.getPropertyOracle().getConfigurationProperty("errai-module-path-append");

                  if (prop.getValues().isEmpty()) {
                    logger.warn("property in gwt.xml file 'errai-module-path-prepend' is undefined");
                    veto.veto();
                    return null;
                  }

                  return prop.getValues().get(0);

                }
                catch (BadPropertyValueException e) {
                  veto.veto();
                  return null;
                }

              }
            } ,
            new DiscoveryStrategy() {
              @Override
              public String getCandidate(GeneratorContext context, DiscoveryVeto veto) {
                // try the CWD
                return new File("").getAbsolutePath();
              }
            }
            ,
            new DiscoveryStrategy() {
              @Override
              public String getCandidate(GeneratorContext context, DiscoveryVeto veto) {
                return RebindUtils.guessWorkingDirectoryForModule(context);
              }
            }
    };
  }

  /**
   * Simple name of class to be generated
   */
  private String className = null;

  /**
   * Package name of class to be generated
   */
  private String packageName = null;
  private TypeOracle typeOracle;
  private String modulePackage;

  @Override
  public String generate(final TreeLogger logger, final GeneratorContext context, final String typeName)
          throws UnableToCompleteException {


    try {
      typeOracle = context.getTypeOracle();

      JClassType classType = typeOracle.getType(typeName);
      packageName = classType.getPackage().getName();
      className = classType.getSimpleSourceName() + "Impl";

      logger.log(TreeLogger.INFO, "Generating Marshallers Bootstrapper...");

      // Generate class source code
      generateMarshallerBootstrapper(logger, context);
    }
    catch (Throwable e) {
      // record sendNowWith logger that Map generation threw an exception
      e.printStackTrace();
      logger.log(TreeLogger.ERROR, "Error generating marshallers", e);
    }

    // return the fully qualified name of the class generated
    return packageName + "." + className;
  }

  public void generateMarshallerBootstrapper(TreeLogger logger, GeneratorContext context) {
    PrintWriter printWriter = context.tryCreate(logger, packageName, className);
    if (printWriter == null) return;
    printWriter.write(_generate(context));
    context.commit(logger, printWriter);
  }

  private String _generate(GeneratorContext context) {
    boolean junit = EnvironmentUtil.isGWTJUnitTest();

    if (junit) {
      System.out.println("******** running inside JUnit! ********");
    }


    if (SERVER_MARSHALLER_OUTPUT_ENABLED) {
      String serverSideClass = MarshallerGeneratorFactory.getFor(MarshallerOuputTarget.Java)
              .generate(SERVER_MARSHALLER_PACKAGE_NAME, SERVER_MARSHALLER_CLASS_NAME);

      if (junit) {
        Random rand = new Random(System.nanoTime());
        String tmpLocation = new File(RebindUtils.getTempDirectory() + "/errai.marshalling/out/").getAbsolutePath();
        System.out.println("*** using temporary path for JUnit Shell: " + tmpLocation + " ***");

        String toLoad = generateServerMarshallers(tmpLocation, serverSideClass);

        try {
          ServerMarshallUtil.loadClassDefinition(toLoad, SERVER_MARSHALLER_PACKAGE_NAME, SERVER_MARSHALLER_CLASS_NAME);
        }
        catch (IOException e) {
          throw new RuntimeException("failed to load server marshallers", e);
        }

      }
      else if (SERVER_MARSHALLER_OUTPUT_DIR != null) {
        generateServerMarshallers(SERVER_MARSHALLER_OUTPUT_DIR, serverSideClass);
        logger.info("** deposited marshaller class in : " + new File(SERVER_MARSHALLER_OUTPUT_DIR).getAbsolutePath());
      }
      else {
        logger.debug("Searching candidate output directories for generated marshallers");
        File outputDirCdt;

        Strategies:
        for (DiscoveryStrategy strategy : rootDiscoveryStrategies) {
          for (String candidate : candidateOutputDirectories) {
            class DiscoveryVetoImpl implements DiscoveryVeto {
              boolean vetoed = false;

              @Override
              public void veto() {
                this.vetoed = true;
              }
            }

            DiscoveryVetoImpl discoveryVeto = new DiscoveryVetoImpl();

            String rootPath = strategy.getCandidate(context, discoveryVeto);

            if (discoveryVeto.vetoed) {
              continue Strategies;
            }

            outputDirCdt = new File(rootPath + candidate);
            if (outputDirCdt.exists()) {
              logger.info("   found '" + outputDirCdt + "' output directory");
              generateServerMarshallers(outputDirCdt.getAbsolutePath(), serverSideClass);
              logger.info("** deposited marshaller class in : " + outputDirCdt.getAbsolutePath());
              break Strategies;
            }
            else {
              logger.debug("   " + outputDirCdt + " does not exist");
            }
          }
        }
      }
    }
    else {
      logger.info("not emitting server marshaller class");
    }


    return MarshallerGeneratorFactory.getFor(MarshallerOuputTarget.GWT).generate(packageName, className);
  }

  interface DiscoveryVeto {
    public void veto();
  }

  interface DiscoveryStrategy {
    public String getCandidate(GeneratorContext context, DiscoveryVeto veto);
  }


  private String generateServerMarshallers(String dir, String serverSideClass) {
    File outputDir = new File(dir + File.separator +
            RebindUtils.packageNameToDirName(SERVER_MARSHALLER_PACKAGE_NAME) + File.separator);
    outputDir.mkdirs();

    File sourceFile = new File(outputDir.getAbsolutePath() + File.separator + SERVER_MARSHALLER_CLASS_NAME + ".java");

    RebindUtils.writeStringToFile(sourceFile,
            serverSideClass);

    ServerMarshallUtil.compileClass(outputDir.getAbsolutePath(), SERVER_MARSHALLER_PACKAGE_NAME, SERVER_MARSHALLER_CLASS_NAME);

    return new File(outputDir.getAbsolutePath() + File.separator + SERVER_MARSHALLER_CLASS_NAME + ".class").getAbsolutePath();
  }
}
