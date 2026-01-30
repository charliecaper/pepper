/*
 * Webpack configuration for Kotlin/JS + Kotlin/Wasm webpack bundles.
 *
 * Background: even_hub_sdk now uses the npm package @evenrealities/even_hub_sdk.
 * Webpack needs to be configured to correctly resolve the scoped package.
 *
 * Note: __dirname at build time points to build/js/packages/Pepper-composeApp/webpack.config.js
 * and we need to traverse up multiple levels to reach the project root.
 */

const path = require("path");
const fs = require("fs");

config.resolve = config.resolve || {};
config.resolve.alias = config.resolve.alias || {};

// Find the project root directory (the one containing node_modules)
// Search upward from the webpack.config.js location until a directory with node_modules is found
function findProjectRoot(startPath) {
    let currentPath = startPath;
    let depth = 0;
    const maxDepth = 10; // Prevent infinite loop
    
    while (currentPath !== path.dirname(currentPath) && depth < maxDepth) {
        const nodeModulesPath = path.join(currentPath, "node_modules");
        if (fs.existsSync(nodeModulesPath)) {
            return currentPath;
        }
        currentPath = path.dirname(currentPath);
        depth++;
    }
    
    // If not found, try common fallback locations
    // build/js/packages/Pepper-composeApp -> project root (7 levels up)
    // build/wasm/packages/Pepper-composeApp -> project root (7 levels up)
    const fallbackRoot = path.resolve(__dirname, "../../../../../../..");
    if (fs.existsSync(path.join(fallbackRoot, "node_modules"))) {
        return fallbackRoot;
    }
    
    // Last resort fallback: assume project root is the parent of composeApp
    return path.resolve(__dirname, "../../../..");
}

const projectRoot = findProjectRoot(__dirname);
const rootNodeModules = path.join(projectRoot, "node_modules");
const sdkPath = path.join(rootNodeModules, "@evenrealities", "even_hub_sdk");

// Verify the path exists
if (fs.existsSync(sdkPath)) {
    // Add alias for scoped package to ensure webpack resolves it correctly
    config.resolve.alias["@evenrealities/even_hub_sdk"] = sdkPath;
    
    // Add node_modules path to the resolve list (ensure dependencies resolve correctly)
    config.resolve.modules = config.resolve.modules || ["node_modules"];
    if (!config.resolve.modules.includes(rootNodeModules)) {
        config.resolve.modules.push(rootNodeModules);
    }
} else {
    console.warn(`Warning: Could not find @evenrealities/even_hub_sdk at ${sdkPath}`);
    console.warn(`Project root resolved to: ${projectRoot}`);
    console.warn(`__dirname is: ${__dirname}`);
}