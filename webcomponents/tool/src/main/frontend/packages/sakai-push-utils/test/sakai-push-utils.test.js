import { setup } from '../src/sakai-push-utils.js';

describe("sakai-push-utils tests", () => {

  it ("sets up uccessfully", async () => {
    setup.then(() => console.log("setup complete"));
  });
});
