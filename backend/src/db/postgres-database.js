import pg from "pg";

const { Pool } = pg;

export function toPostgresPlaceholders(sql) {
  let index = 0;
  return sql.replace(/\?/g, () => "$" + (++index));
}

export class PostgresDatabase {
  constructor(pool) {
    this.pool = pool;
    this.dialect = "postgres";
  }

  static fromConnectionString(connectionString) {
    if (!connectionString) throw new Error("DATABASE_URL is required for PostgreSQL.");
    return new PostgresDatabase(new Pool({
      connectionString,
      ssl: process.env.PGSSLMODE === "require" ? { rejectUnauthorized: true } : undefined,
      max: Number(process.env.PG_POOL_MAX || 10),
      idleTimeoutMillis: 30000,
      connectionTimeoutMillis: 10000
    }));
  }

  async all(sql, ...params) {
    const result = await this.pool.query(toPostgresPlaceholders(sql), params);
    return result.rows;
  }

  async get(sql, ...params) {
    const rows = await this.all(sql, ...params);
    return rows[0];
  }

  async run(sql, ...params) {
    const result = await this.pool.query(toPostgresPlaceholders(sql), params);
    return { changes: result.rowCount };
  }

  async exec(sql) {
    return this.pool.query(sql);
  }

  async close() {
    await this.pool.end();
  }
}
