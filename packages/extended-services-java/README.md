## Extended Services

This is a shallow copy of the Extended-Services Go application,
but it uses Java instead.

## Running

You can run it on developement mode:

- `pnpm start`

## Parameters

- `-Dip=<PORT_NUMBER>`: Sets app port, the default value is `21345`.
- `-Dport=<HOST>`: Sets app host, the default value is `0.0.0.0`.
- `-DkieSandboxUrl=<URL>`: Sets kie.sandbox.url host, the default value is `https://localhost:9001`.

### API

#### `/`

The root route is a proxy and will forward your requests to the autogenerated port

#### `/ping`

[GET] returns the API version, the proxy port and IP and the KIE Sandbox URL.

```json
{
  "version": "0.0.0",
  "proxy": {
    "ip": "0.0.0.0",
    "port": "21345"
  },
  "kieSandboxUrl": "https://localhost:9001",
  "started": true
}
```
