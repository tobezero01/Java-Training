interface Product {
  id: number;
  name: string;
  price: number;
}

class Book implements Product {
  constructor(
    public id: number,
    public name: string,
    public price: number,
    public author: string
  ) {
    if (!Number.isInteger(id) || id <= 0) {
      throw new Error("id phải là số nguyên dương");
    }
    if (typeof name !== "string" || name.trim().length < 2) {
      throw new Error("name tối thiểu 2 ký tự");
    }
    if (typeof author !== "string" || author.trim().length < 2) {
      throw new Error("author tối thiểu 2 ký tự");
    }
    if (!Number.isFinite(price) || price < 0) {
      throw new Error("price phải là số >= 0");
    }
  }

  info(): string {
    return `#${this.id} — "Name : ${this.name}" ; Tác giả: ${this.author} ; Giá: ${this.price} VND`;
  }
}

const book1 = new Book(1, "Clean Code", 325000, "Martin");
console.log(book1.info());

const books: Book[] = [
  book1, new Book(2, "Đắc Nhân Tâm", 999000, "Duc"),
];
books.forEach((b) => console.log(b.info()));
