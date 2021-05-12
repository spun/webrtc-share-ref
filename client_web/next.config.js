const ESLintPlugin = require('eslint-webpack-plugin');
module.exports = {
  webpack: (config, { }) => {
    config.plugins.push(new ESLintPlugin({ context: 'src/', extensions: ['js', 'jsx', 'ts', 'tsx'], failOnError: false }))
    return config
  },
  distDir: 'build',
  future: {
    webpack5: true,
  },
};
