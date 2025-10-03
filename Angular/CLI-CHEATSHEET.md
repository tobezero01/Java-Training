# Cẩm nang CLI: Node.js, npm, npx, Angular CLI (ng), TypeScript

## 1) Node.js

```bash
node -v                              # xem phiên bản Node
node file.js                         # chạy file JS
node --watch file.js                 # auto-reload khi file đổi (Node 18+)
node --inspect file.js               # debug qua DevTools
node --inspect-brk file.js           # debug và dừng ở dòng đầu
node                                  # mở REPL (gõ JS trực tiếp)
```

**Quản lý phiên bản Node bằng nvm:**
```bash
nvm ls                               # liệt kê phiên bản đã cài
nvm install 20                       # cài Node 20
nvm use 20                           # chuyển sang Node 20
```

---

## 2) npm (Node Package Manager)

```bash
npm init -y                          # tạo package.json nhanh
npm i <pkg>                          # cài dependencies (prod)
npm i -D <pkg>                       # cài devDependencies
npm uninstall <pkg>                  # gỡ gói
npm run <script>                     # chạy script trong package.json
npm run                              # liệt kê các script
npm outdated                         # gói nào đã cũ
npm update                           # cập nhật theo range (^, ~)
npm audit                            # quét bảo mật
npm audit fix                        # tự sửa nếu được
npm cache clean --force              # dọn cache nếu lỗi kỳ quặc
npm link / npm unlink                # link gói local để dev chéo
npm pack                             # đóng gói .tgz (trước khi publish)
npm login && npm whoami              # đăng nhập + kiểm tra tài khoản
npm version patch|minor|major        # bump version + git tag
npm publish --access public          # publish (package public)
npm ci                               # cài sạch theo lockfile (CI)
```

**Mẹo nhanh:**
```bash
# Khi lỗi khó hiểu:
rm -rf node_modules package-lock.json
npm i
# Đừng commit node_modules; luôn commit package-lock.json
```

---

## 3) npx (chạy CLI tạm thời, không cần cài global)

```bash
npx <cli>                            # chạy CLI từ registry
npx <cli>@latest                     # chạy bản mới nhất

# ví dụ:
npx create-vite@latest               # scaffold Vite app
npx eslint --init                    # khởi tạo ESLint
npx prettier . --write               # format code
```

> Khác biệt: `npm i -g <cli>` = cài **global**. `npx <cli>` = chạy **ngay**, không cần cài.

---

## 4) Angular CLI (ng)  *(lưu ý: CLI là `ng`, không phải `ngx`)*

```bash
npm i -g @angular/cli                # cài global
ng v                                 # xem version
ng new my-app                        # tạo project Angular
cd my-app
ng serve -o                          # chạy dev & mở browser
ng generate component home           # tạo component
ng build                             # build production (dist/)
ng test                              # unit test
ng e2e                               # e2e tests
ng add <pkg>                         # thêm plugin (PWA, Material,…)
ng update @angular/core @angular/cli # cập nhật Angular
```

---

## 5) TypeScript (tsc, ts-node, tsx)

**Cài & khởi tạo:**
```bash
npm i -D typescript
npx tsc --init                       # tạo tsconfig.json
```

**Biên dịch & chạy:**
```bash
npx tsc                              # biên dịch theo tsconfig (src -> dist)
npx tsc src/index.ts                 # biên dịch 1 file
node dist/index.js                   # chạy JS đã build
```

**Chạy TS không cần build (tiện dev):**
```bash
npm i -D ts-node
npx ts-node src/index.ts

# hoặc dùng tsx (khuyên dùng)
npm i -D tsx
npx tsx src/index.ts
npx tsx --watch src/index.ts         # auto-reload
```

**`tsconfig.json` mẫu (cơ bản):**
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "rootDir": "src",
    "outDir": "dist"
  },
  "include": ["src"]
}
```

---

## 6) Dự án mẫu nhanh

**(a) Node + TS tối giản**
```bash
mkdir my-ts && cd my-ts
npm init -y
npm i -D typescript tsx
npx tsc --init
mkdir src
printf "export const sum=(a:number,b:number)=>a+b;
console.log(sum(2,3));
" > src/index.ts
npx tsx src/index.ts                 # chạy TS trực tiếp
```

**(b) React + Vite (ESM)**
```bash
npx create-vite@latest my-react --template react-ts
cd my-react
npm i
npm run dev
```

**(c) Angular**
```bash
npm i -g @angular/cli
ng new my-ng --routing --style=scss
cd my-ng
ng serve -o
```

---

## 7) Scripts hữu ích trong package.json

```json
{
  "scripts": {
    "dev": "tsx --watch src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js",
    "lint": "eslint .",
    "format": "prettier . --write",
    "test": "vitest"
  }
}
```

---

## 8) Kiến thức nhanh cần nhớ

- **ESM vs CJS**  
  - ESM: `import/export` (thời nay), cần `"type":"module"` trong package.json **hoặc** dùng `.mjs`.  
  - CJS: `require/module.exports` (kiểu cũ).
- **npx** = chạy CLI ngay không cần cài global.  
- **npm ci** cho CI/CD (nhanh & chuẩn theo lockfile).  
- **Angular CLI** dùng `ng ...`, *không phải* `ngx`.  
- **TypeScript** chỉ thêm **type** lúc build-time; runtime vẫn là JS.

---

## 9) Troubleshooting nhanh

```bash
# Lỗi module ESM/CJS (import/require):
# - Kiểm tra "type":"module" trong package.json
# - Dùng đúng đuôi .mjs/.cjs hoặc sửa tsconfig module/moduleResolution

# Gói không có type:
npm i -D @types/<pkg>

# Xung đột version Node:
nvm use <version>

# Build sai đường dẫn:
# - Kiểm tra rootDir/outDir trong tsconfig
# - Xoá dist/ rồi build lại
```

> Gợi ý: lưu file này trong dự án (ví dụ `CLI-CHEATSHEET.md`) để team copy dùng nhanh.
