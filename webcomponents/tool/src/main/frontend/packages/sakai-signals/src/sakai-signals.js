import { Signal } from "signal-polyfill";

export const userChanged = new Signal.State({});
export const loggedOut = new Signal.State(1);
