const fs = require('fs');

const data = JSON.parse(fs.readFileSync('Library API.postman_collection.json', 'utf8'));

const booksFolder = data.item.find(i => i.name === 'Books');
if (booksFolder && booksFolder.item) {
  booksFolder.item = booksFolder.item.filter(i => i.name !== 'Increment Book View');
}

fs.writeFileSync('Library API.postman_collection.json', JSON.stringify(data, null, 2));
console.log('Removed Increment Book View from Postman collection.');
