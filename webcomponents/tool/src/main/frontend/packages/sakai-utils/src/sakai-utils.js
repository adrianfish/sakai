import { SourceMapConsumer } from "source-map-js";

const sourceMaps = {};

const getSourceMapFromUri = async uri => {

  if (sourceMaps[uri]) return sourceMaps[uri];

  const uriQuery = new URL(uri).search;
  const currentScriptContent = await (await fetch(uri)).text();

  let mapUri = RegExp(/\/\/# sourceMappingURL=(.*)/).exec(currentScriptContent)[1];
  mapUri = new URL(mapUri, uri).href + uriQuery;

  const map = await (await fetch(mapUri)).json();

  if (!sourceMaps[uri]) sourceMaps[uri] = map;

  return map;
};

const mapStackTrace = async stack => {

  const stackLines = stack.split("\n");
  const mappedStack = [];

  for (const line of stackLines) {
    const match = RegExp(/(.*)(https?:\/\/.*):(\d+):(\d+)/).exec(line);
    if (match == null) {
      mappedStack.push(line);
      continue;
    }

    const uri = match[2];
    const consumer = new SourceMapConsumer(await getSourceMapFromUri(uri));

    const originalPosition = consumer.originalPositionFor({
      line: parseInt(match[3]),
      column: parseInt(match[4]),
    });

    if (originalPosition.source == null || originalPosition.line == null || originalPosition.column == null) {
      mappedStack.push(line);
      continue;
    }

    mappedStack.push(`${originalPosition.source}:${originalPosition.line}:${originalPosition.column + 1}`);
  }

  return mappedStack.join("\n");
};

export const handleError = e => {

  const stack = e?.stack || e?.reason?.stack;

  if (!stack) return false;

  mapStackTrace(stack).then(t => console.log(t));
  return false;
};
