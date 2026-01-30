// Allow access from other devices on the local network (e.g. Even App on iPhone)
if (config.devServer) {
    config.devServer.host = '0.0.0.0';
    config.devServer.allowedHosts = 'all';
}
