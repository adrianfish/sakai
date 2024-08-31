var frontendLogger = frontendLogger || {};

frontendLogger.sourceMaps = {};

// Lifted frontendLogger from https://stackoverflow.com/questions/75400967/get-stack-trace-as-a-string-when-using-source-map
frontendLogger.getSourceMapFromUri = async uri => {

  if (frontendLogger.sourceMaps[uri]) return frontendLogger.sourceMaps[uri];

  const uriQuery = new URL(uri).search;
  const currentScriptContent = await (await fetch(uri)).text();

  let mapUri = RegExp(/\/\/# sourceMappingURL=(.*)/).exec(currentScriptContent)[1];
  mapUri = new URL(mapUri, uri).href + uriQuery;

  const map = await (await fetch(mapUri)).json();

  if (!frontendLogger.sourceMaps[uri]) frontendLogger.sourceMaps[uri] = map;

  return map;
};

// Lifted frontendLogger from https://stackoverflow.com/questions/75400967/get-stack-trace-as-a-string-when-using-source-map
frontendLogger.mapStackTrace = async stack => {

  const stackLines = stack.split("\n");
  const mappedStack = [];

  for (const line of stackLines) {
    const match = RegExp(/(.*)(https?:\/\/.*):(\d+):(\d+)/).exec(line);
    if (match == null) {
      mappedStack.push(line);
      continue;
    }

    const uri = match[2];

    const mapping = await sourceMap.SourceMapConsumer.with(await frontendLogger.getSourceMapFromUri(uri), null, consumer => {

      const originalPosition = consumer.originalPositionFor({
        line: parseInt(match[3]),
        column: parseInt(match[4]),
      });

      if (originalPosition.source == null || originalPosition.line == null || originalPosition.column == null) {
        return line;
      }

      return `${originalPosition.source}:${originalPosition.line}:${originalPosition.column + 1}`;
    });

    mappedStack.push(mapping);
  }

  return mappedStack.join("\n");
};

frontendLogger.logError = t => {

  const url = "/api/frontend-logger";
  fetch(url, {
    method: "POST",
    body: t,
  })
  .catch(e => console.debug(e));
};

frontendLogger.handleError = e => {

  const stack = e?.stack || e?.reason?.stack;

  if (!stack) {
    frontendLogger.logError(`${e.message} ${e.filename}:${e.lineno}:${e.colno}`);
  } else {
    frontendLogger.mapStackTrace(stack).then(t => frontendLogger.logError(t));
  }

  return false;
};

window.addEventListener("unhandledrejection", frontendLogger.handleError);
window.addEventListener("error", frontendLogger.handleError);
