Happy Server
============

A _very_ simple server that parses incoming requests, logs them, and returns `200 OK` if it was able to parse the request.

This is mostly a debugging helper.

HTTPS Support
-------------

You'll need to provide the certs to enable this. As this is intended to be used as a debugging tool, it does not attempt
to access the `cacerts`, and you'll need to provide a keystore.

### Generating a keystore

The easiest way is to use the `keytool` that ships the JDK:
```bash
keytool -genkey -alias happy-server-cert -keyalg RSA -validity 365 -sigalg SHA256withRSA -storepass i-am-happy
```
Note: the alias doesn't matter, but you will almost certainly want to answer "localhost" for the 
question "What is your first and last name?", unless as that's the domain name.

### Providing the keystore to Happy Server

When you start Happy Server, you'll need to provide the keystore file and passwords:
```bash
mill __.runMain peschke.happy.CliApp --port 8081 --tls-store keystore.jks --tls-pass i-am-happy
```

### Connecting over TLS

In this example, we'll connect using cURL. Each tool is going to be a little different, so adapt as necessary.

Unless your tool is also running on the `java.security` framework, you'll likely need to export the cert in a form 
that it can use. For cURL, this is PEM:
```bash
keytool -export -rfc -alias happy-server-cert -file happy-server-cert.pem -keystore keystore.jks -storepass i-am-happy
```

This PEM file then needs to be provided to cURL: 
```bash
$ curl -v https://localhost:8081/ --cacert ./happy-server-cert.pem
```