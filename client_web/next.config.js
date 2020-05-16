// Example config for adding a loader that depends on babel-loader
// This source was taken from the @next/mdx plugin source:
// https://github.com/zeit/next.js/tree/canary/packages/next-mdx
module.exports = {
  webpack: (config, options) => {
    config.module.rules.push({
      test: /\.js$/,
      exclude: /node_modules/,
      use: [
        options.defaultLoaders.babel,
        { loader: 'eslint-loader', options: { emitWarning: true } },
      ],
    });

    return config;
  },
};

module.exports = {
  distDir: 'build',
};

module.exports = {
  target: 'serverless',
};
