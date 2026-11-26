import crypto from "node:crypto";
import jwt from "jsonwebtoken";
import { CatalogueProvider, IdentityProvider, EntitlementProvider, PlaybackProvider } from "./contracts.js";

export class PostgresCatalogueProvider extends CatalogueProvider {
  constructor(db) { super(); this.db = db; }
  async listCataloguePage(page=1,pageSize=20) {
    const offset=(page-1)*pageSize;
    const count=await this.db.get("SELECT COUNT(*) AS total FROM subjects");
    const total=Number(count.total);
    const batches=await this.db.all("SELECT id,name AS title,name FROM subjects ORDER BY name,id LIMIT ? OFFSET ?",pageSize,offset);
    const items=[];
    for (const batch of batches) {
      const courses=await this.db.all("SELECT id,title,description,visibility,batch_id AS \"batchId\" FROM courses WHERE batch_id=? ORDER BY title,id",batch.id);
      const nestedCourses=[];
      for (const course of courses) {
        const chapters=await this.db.all("SELECT id,course_id AS \"courseId\",title,position FROM course_chapters WHERE course_id=? ORDER BY position,title,id",course.id);
        const nestedChapters=[];
        for (const chapter of chapters) {
          const lectures=await this.db.all("SELECT id,chapter_id AS \"chapterId\",title,description,lecture_type AS type,visibility FROM lectures WHERE chapter_id=? AND lecture_type='recorded' ORDER BY title,id",chapter.id);
          nestedChapters.push({...chapter,lectures});
        }
        nestedCourses.push({...course,chapters:nestedChapters});
      }
      items.push({...batch,courses:nestedCourses});
    }
    return {items,page,pageSize,totalItems:total,totalPages:Math.ceil(total/pageSize),hasNextPage:offset+items.length<total};
  }
  listBatches() { return this.db.all("SELECT id,name AS title,name FROM subjects ORDER BY name"); }
  listCourses(batchId) { return batchId ? this.db.all("SELECT id,title,description,visibility,batch_id AS \"batchId\" FROM courses WHERE batch_id=? ORDER BY title", batchId) : this.db.all("SELECT id,title,description,visibility,batch_id AS \"batchId\" FROM courses ORDER BY title"); }
  getCourse(id) { return this.db.get("SELECT id,title,description,visibility,batch_id AS \"batchId\" FROM courses WHERE id=?", id).then(row => row ?? null); }
  listChapters(courseId) { return this.db.all("SELECT id,course_id AS \"courseId\",title,position FROM course_chapters WHERE course_id=? ORDER BY position,title", courseId); }
  listLectures(chapterId,type="recorded") { return this.db.all("SELECT id,chapter_id AS \"chapterId\",title,description,lecture_type AS type,visibility FROM lectures WHERE chapter_id=? AND lecture_type=? ORDER BY title", chapterId, type); }
  listLiveClasses() { return this.db.all("SELECT id,title,course_id AS \"courseId\",starts_at AS \"startsAt\",status,visibility FROM live_classes ORDER BY starts_at"); }
}

function digest(password, salt=crypto.randomBytes(16).toString("hex")) {
  return { salt, hash: crypto.scryptSync(password,salt,64).toString("hex") };
}
function verify(password, stored) {
  const [salt,hash]=String(stored).split(":");
  if(!salt||!hash)return false;
  const actual=Buffer.from(digest(password,salt).hash,"hex"), expected=Buffer.from(hash,"hex");
  return actual.length===expected.length && crypto.timingSafeEqual(actual,expected);
}

export class PostgresIdentityProvider extends IdentityProvider {
  constructor(db,jwtSecret) { super(); this.db=db; this.jwtSecret=jwtSecret; }
  token(user) { return { accessToken: jwt.sign({sub:user.id,email:user.email},this.jwtSecret,{expiresIn:"30m",issuer:"balidan-rwa-api"}),tokenType:"Bearer" }; }
  async register({name,email,password}) {
    const id=crypto.randomUUID(), {salt,hash}=digest(password), normalized=email.trim().toLowerCase();
    await this.db.run("INSERT INTO users(id,name,email,password_hash,created_at) VALUES(?,?,?,?,?)",id,name.trim(),normalized,salt+":"+hash,new Date().toISOString());
    return this.token({id,email:normalized});
  }
  async authenticate({email,password}) {
    const user=await this.db.get("SELECT id,email,password_hash FROM users WHERE LOWER(email)=LOWER(?)",email.trim());
    if(!user||!verify(password,user.password_hash))return null;
    return this.token(user);
  }
}

export class PostgresEntitlementProvider extends EntitlementProvider {
  constructor(db) { super(); this.db=db; }
  async check(userId,contentId) {
    const row=await this.db.get("SELECT decision,expires_at AS \"expiresAt\" FROM entitlement_checks WHERE user_id=? AND content_id=? ORDER BY checked_at DESC LIMIT 1",userId,contentId);
    if(!row||(row.expiresAt&&Date.parse(row.expiresAt)<=Date.now()))return {contentId,decision:"unknown"};
    return {contentId,decision:row.decision};
  }
}

export class UnconfiguredPostgresPlaybackProvider extends PlaybackProvider {
  async createSession(_userId,contentId) {
    return {contentId,status:"unavailable",reason:"No authorized playback provider is configured."};
  }
}
