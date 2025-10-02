/**
 * Cách dùng:
 *  - Node:   node arrays-and-methods-demo.js
 *  - Browser: copy toàn bộ nội dung vào <script type="module"> trong HTML
 *
 * Mục tiêu: Minh hoạ các thao tác mảng cơ bản và hiện đại
 *  - Tạo mảng: literal, Array.of, Array.from
 *  - Thêm/xoá: push/pop, unshift/shift, splice
 *  - Truy cập/lấy phần: at, slice
 *  - Kết hợp/biến đổi: concat, map, filter, reduce, flat, flatMap
 *  - Tra cứu/kiểm tra: find, findIndex, some, every, includes, indexOf
 *  - Sắp xếp: sort, toSorted (nếu runtime hỗ trợ), reverse, toReversed
 *  - Sao chép bất biến (immutable) bằng spread [...]
 */

function headline(title) {
    const bar = "-".repeat(title.length);
    console.log(`\n${title}\n${bar}`);
}

// 1) Tạo mảng
headline("1) Tạo mảng");
const a1 = [1, 2, 3];
const a2 = Array.of(4, 5, 6);
const a3 = Array.from("hello", ch => ch.toUpperCase()); // ['H','E','L','L','O']
console.log({ a1, a2, a3 });

// 2) Thêm / xoá phần tử (mutating)
headline("2) Thêm/Xoá phần tử (mutating)");
const m = [1];
m.push(2);            // [1,2]
const last = m.pop(); // pop→2, m:[1]
m.unshift(0);         // [0,1]
const first = m.shift(); // shift→0, m:[1]
console.log({ m, first, last });

// splice(start, deleteCount, ...items) – vừa xoá vừa chèn (mutating)
const sp = [10, 20, 30, 40];
const removed = sp.splice(1, 2, 25, 27); // xoá 20,30; chèn 25,27 → [10,25,27,40]
console.log({ sp, removed });

// 3) Truy cập & lấy phần
headline("3) Truy cập & lấy phần");
const b = ["a", "b", "c", "d", "e"];
console.log("b.at(0)", b.at(0));       // 'a'
console.log("b.at(-1)", b.at(-1));     // 'e' (âm: từ cuối)
console.log("b.slice(1,4)", b.slice(1, 4)); // ['b','c','d'] (không mutate)

// 4) Kết hợp & sao chép
headline("4) Kết hợp & sao chép");
const c1 = [1, 2], c2 = [3, 4];
const c3 = c1.concat(c2);     // [1,2,3,4]
const c4 = [...c1, 99, ...c2]; // spread: bất biến
console.log({ c3, c4, c1_unchanged: c1 });

// 5) Biến đổi với map/filter/reduce
headline("5) map / filter / reduce");
const nums = [1, 2, 3, 4, 5];
const squared = nums.map(x => x * x);            // [1,4,9,16,25]
const evens = nums.filter(x => x % 2 === 0);       // [2,4]
const sum = nums.reduce((acc, x) => acc + x, 0); // 15
console.log({ nums, squared, evens, sum });

// 6) Tra cứu & kiểm tra
headline("6) Tra cứu & kiểm tra");
const people = [
    { id: 1, name: "Nhu", age: 18 },
    { id: 2, name: "Lan", age: 20 },
    { id: 3, name: "Minh", age: 16 },
];
const found = people.find(p => p.id === 2);         // phần tử
const foundIdx = people.findIndex(p => p.age < 18); // vị trí đầu tiên
const anyAdult = people.some(p => p.age >= 18);     // true/false
const allAdult = people.every(p => p.age >= 18);    // true/false
console.log({ found, foundIdx, anyAdult, allAdult });

// includes / indexOf cho mảng giá trị nguyên thuỷ
const letters = ["a", "b", "c", "b"];
console.log({ includes_b: letters.includes("b"), indexOf_b: letters.indexOf("b"), lastIndexOf_b: letters.lastIndexOf("b") });

// 7) flat / flatMap – san phẳng & biến đổi
headline("7) flat / flatMap");
const nested = [1, [2, [3, 4]], 5];
console.log("flat(1)", nested.flat(1)); // [1,2,[3,4],5]
console.log("flat(2)", nested.flat(2)); // [1,2,3,4,5]

const words = ["xin ", "chào", " bạn"]; // loại bỏ khoảng trắng thừa và tách ký tự
const chars = words.flatMap(w => w.trim().split(""));
console.log({ chars });

// 8) sort / toSorted / reverse / toReversed
headline("8) sort / toSorted / reverse / toReversed");
const s1 = [3, 1, 11, 2];
s1.sort((a, b) => a - b); // MUTATE s1
console.log({ s1_sorted_in_place: s1 });

// Nếu môi trường hỗ trợ (Node 20+/modern browser), toSorted/toReversed trả về bản SAO CHÉP, không mutate
const s2 = [3, 1, 11, 2];
const s2_sorted_copy = s2.toSorted ? s2.toSorted((a, b) => a - b) : [...s2].sort((a, b) => a - b);
console.log({ s2_original: s2, s2_sorted_copy });

const r1 = [1, 2, 3];
const r1_reversed_in_place = r1.reverse(); // MUTATE
const r2 = [1, 2, 3];
const r2_reversed_copy = r2.toReversed ? r2.toReversed() : [...r2].reverse();
console.log({ r1, r1_reversed_in_place, r2, r2_reversed_copy });

// 9) Thực hành nhỏ: tính tổng giá, lọc, gom nhóm
headline("9) Bài thực hành nhỏ");
const products = [
    { id: 1, name: "Pen", price: 2.5, category: "office" },
    { id: 2, name: "Book", price: 5.0, category: "office" },
    { id: 3, name: "Tea", price: 1.5, category: "food" },
    { id: 4, name: "Snack", price: 2.0, category: "food" },
];

const totalPrice = products.reduce((acc, p) => acc + p.price, 0);
const cheap = products.filter(p => p.price < 2.5).map(p => p.name);
// Gom nhóm theo category → { office:[...], food:[...] }
const byCategory = products.reduce((acc, p) => {
    (acc[p.category] ||= []).push(p); return acc;
}, {});
console.log({ totalPrice, cheap, byCategory });

// 10) Sao chép bất biến khi cập nhật phần tử trong mảng object
headline("10) Cập nhật bất biến với spread");
const updated = products.map(p => p.id === 2 ? { ...p, price: p.price + 1 } : p);
console.log({ updated, products_kept_immutable: products });

// 11) forEach vs map (khác nhau)
headline("11) forEach vs map");
const forEachArr = [];
[1, 2, 3].forEach(x => forEachArr.push(x * 2)); // side-effect
const mapArr = [1, 2, 3].map(x => x * 2);       // trả về mảng mới
console.log({ forEachArr, mapArr });

// 12) Edge cases: sparse array, holes
headline("12) Edge cases: sparse array");
const sparse = [1, , 3]; // có "lỗ" ở index 1
console.log({ length: sparse.length, has1: (1 in sparse), mapped: sparse.map(x => x ?? "_") });

console.log("\nDone.\n");
