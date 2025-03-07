# Sakai Web Components

This project hosts a set of cross-cutting web components used across several Sakai tools. The
project uses modern JavaScript tooling with Lit for component development, @web/test-runner for
testing, ESLint for code quality and esbuild for bundling.

## Development Setup

1. Install Node.js and npm from [https://nodejs.org/en/download/](https://nodejs.org/en/download/)
2. Navigate to the frontend directory:
   ```
   cd SAKAI_SRC/webcomponents/tool/src/main/frontend
   ```
3. Install dependencies:
   ```
   npm install
   ```

## Available Commands

Run these commands from the `SAKAI_SRC/webcomponents/tool/src/main/frontend` directory:

- **Linting**:
  ```
  npm run lint
  ```
  Runs ESLint against JavaScript files with strict error checking

- **Bundling**:
  ```
  npm run bundle
  ```
  Bundles JS files using esbuild with source maps and minification. If you don't want to wait for a
  full maven build, you can copy these bundles directly into your tomcat directory.

- **Type Checking**:
  ```
  npm run analyze
  ```
  Runs lit-analyzer for static type checking of Lit components

- **Testing**:
  ```
  npm test
  ```
  Run component tests with @web/test-runner

## Building

Build this project like any other Sakai project.

```
mvn clean install sakai:deploy
```

This will build the api, bundle and tool/frontend components, and deploy the built artifacts to
your tomcat. If you've made changes to the bundle you will need to restart tomcat for the changes
to take effect. If you've made changes to the frontend components you will need to refresh the
browser to see the changes. As you make changes to the frontend components you could just run the
bundle command and copy your bundles directly into your deployed webcomponents directory.

## Project Structure

The project uses a monorepo structure managed by Lerna:

- `/packages/` - Contains individual component packages
- Each component package follows this structure:
  - `/src/` - Component source files
  - `/test/` - Test files
  - `package.json` - Component metadata and dependencies
  - `web-test-runner.config.mjs` - Test configuration

## Creating a New Component

1. Create a new directory in the `packages/` directory for your component
2. Create the following files:
   - `package.json` - Define your component's metadata and dependencies
   - `index.js` - Export your component
   - `src/YourComponent.js` - Implement your component using Lit
   - `test/your-component.test.js` - Write tests for your component
   - `web-test-runner.config.mjs` - Configure testing

3. Follow existing components as templates for proper structure and conventions
4. Write your component and tests in parallel for better code coverage

## Best Practices

- Use modern JavaScript (ES6+) features
- Follow web component and Lit standards
- Support internationalization through the sakai-i18n package
- Ensure components are accessible and responsive
- Write comprehensive tests for your components

[Tutorial: Build a webcomponent in Sakai](docs/tutorial.md)
