// 只初始化固定的本地联调库，不读取或修改 application-dev 的远程数据库。
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const mysql = process.env.LOCAL_MYSQL_BIN || 'mysql';
const database = 'training_career_local';
const sqlRoot = path.join(root, 'src/main/resources/sql');
function run(sql, useDatabase = true) {
  const result = spawnSync(mysql, ['-h', '127.0.0.1', '-P', '3306', '-u', 'root', '--default-character-set=utf8mb4', '--batch', ...(useDatabase ? [database] : [])], {
    input: sql, encoding: 'utf8', env: { ...process.env, MYSQL_PWD: process.env.LOCAL_MYSQL_ROOT_PASSWORD || '123456' }
  });
  if (result.status !== 0) throw new Error(result.stderr || 'mysql failed');
  return result.stdout;
}
run("CREATE DATABASE IF NOT EXISTS " + database + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS 'training_local'@'127.0.0.1' IDENTIFIED BY 'training-local-db'; GRANT ALL PRIVILEGES ON " + database + ".* TO 'training_local'@'127.0.0.1';", false);
const files = ['init.sql', 'init_permission_data.sql', 'add_student_portal_permission.sql',
  ...fs.readdirSync(sqlRoot).filter(name => name.startsWith('add_') && name !== 'add_student_portal_permission.sql').sort(),
  'adapt_training_career_platform.sql'];
for (const file of files) {
  run(fs.readFileSync(path.join(sqlRoot, file), 'utf8'));
  console.log('Applied ' + file);
}
console.log(run("SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema=DATABASE();"));
