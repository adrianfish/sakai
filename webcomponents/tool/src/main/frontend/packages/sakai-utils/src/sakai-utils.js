import { RawSourceMap, SourceMapConsumer } from 'source-map-js';

const sourceMaps = {};
async function getSourceMapFromUri(uri) {

  if (sourceMaps[uri]) return sourceMaps[uri];

  const uriQuery = new URL(uri).search;
  const currentScriptContent = await (await fetch(uri)).text();

  let mapUri = RegExp(/\/\/# sourceMappingURL=(.*)/).exec(currentScriptContent)[1];
  mapUri = new URL(mapUri, uri).href + uriQuery;

  const map = await (await fetch(mapUri)).json();

  sourceMaps[uri] = map;

  return map;
}

export async function mapStackTrace(stack) {

  const stackLines = stack.split('\n');
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

  return mappedStack.join('\n');
}
