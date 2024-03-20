# sakai-signals

A set of utility functions for setting up browser push in Sakai.

## Installation

```bash
npm i @sakai-ui/sakai-signals
```

## Usage

```html
import { setup } from "@sakai-ui/sakai-signals";

setup.then(() => console.log("push setup complete"));
```

## Linting and formatting

To scan the project for linting and formatting errors, run

```bash
npm run lint
```

To automatically fix linting and formatting errors, run

```bash
npm run lint:fix
```

## Testing with Web Test Runner

To execute the tests for this module, run

```bash
npm run test
```
To run the tests in interactive watch mode run:

```bash
npm run test:watch
```
