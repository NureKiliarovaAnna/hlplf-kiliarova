const http = require('http');
const fs = require('fs');
const path = require('path');

const host = 'localhost';
const port = 8080;
const root = __dirname;

const contentTypes = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8',
};

function getFilePath(url) {
  const cleanUrl = decodeURIComponent(url.split('?')[0]);
  const requestedPath = cleanUrl === '/' ? '/index.html' : cleanUrl;
  return path.join(root, requestedPath);
}

const server = http.createServer((request, response) => {
  const filePath = getFilePath(request.url);

  if (!filePath.startsWith(root)) {
    response.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
    response.end('403 Forbidden');
    return;
  }

  fs.readFile(filePath, (error, data) => {
    if (error) {
      response.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      response.end('404 Not Found');
      return;
    }

    const extension = path.extname(filePath);
    response.writeHead(200, {
      'Content-Type': contentTypes[extension] || 'application/octet-stream',
    });
    response.end(data);
  });
});

server.listen(port, host, () => {
  console.log(`Локальний сервер запущено: http://${host}:${port}`);
  console.log('Для зупинки натисніть Ctrl+C у терміналі.');
});
