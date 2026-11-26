import crypto from "node:crypto";
import jwt from "jsonwebtoken";
import { CatalogueProvider, IdentityProvider, EntitlementProvider, PlaybackProvider } from "./contracts.js";

export class SqliteCatalogueProvider extends CatalogueProvider {
  constructor(db) { super(); this.db = db; }
  listCataloguePage(page=1,pageSize=20) {
    const offset=(page-1)*pageSize;
    const total=this.db.prepare("SELECT COUNT(*) AS total FROM subjects").get().total;
    const batches=this.db.prepare("SELECT id,name AS title,name FROM subjects ORDER BY name,id LIMIT ? OFFSET ?").all(pageSize,offset);
    const items=batches.map(batch=>{
      const courses=this.db.prepare("SELECT id,title,description,visibility,batch_id AS batchId FROM courses WHERE batch_id=? ORDER BY title,id").all(batch.id);
      const nestedCourses=courses.map(course=>{
        const chapters=this.db.prepare("SELECT id,course_id AS courseId,title,position FROM course_chapters WHERE course_id=? ORDER BY position,title,id").all(course.id);
        return {...course,chapters:chapters.map(chapter=>({...chapter,lectures:this.db.prepare("SELECT id,chapter_id AS chapterId,title,description,lecture_type AS type,visibility FROM lectures WHERE chapter_id=? AND lecture_type='recorded' ORDER BY title,id").all(chapter.id)}))};
      });
      return {...batch,courses:nestedCourses};
    });
    return {items,page,pageSize,totalItems:total,totalPages:Math.ceil(total/pageSize),hasNextPage:offset+items.length<total};
  }
  listBatches() { return this.db.prepare("SELECT id,name AS title,name FROM subjects ORDER BY name").all(); }
  listCourses(batchId) {
    return this.db.prepare("SELECT id,title,description,visibility,batch_id AS batchId FROM courses WHERE (? IS NULL OR batch_id=?) ORDER BY title").all(batchId ?? null,batchId ?? null);
  }
  getCourse(id) { return this.db.prepare("SELECT id,title,description,visibility,batch_id AS batchId FROM courses WHERE id=?").get(id) ?? null; }
  listChapters(courseId) { return this.db.prepare("SELECT id,course_id AS courseId,title,position FROM course_chapters WHERE course_id=? ORDER BY position,title").all(courseId); }
  listLectures(chapterId,type="recorded") {
    return this.db.prepare("SELECT id,chapter_id AS chapterId,title,description,lecture_type AS type,visibility FROM lectures WHERE chapter_id=? AND lecture_type=? ORDER BY title").all(chapterId,type);
  }
  listLiveClasses() { return this.db.prepare("SELECT id,title,course_id AS courseId,starts_at AS startsAt,status,visibility FROM live_classes ORDER BY starts_at").all(); }
}
function digest(password,salt=crypto.randomBytes(16).toString("hex")) {
  return { salt, hash: crypto.scryptSync(password,salt,64).toString("hex") };
}
function verify(password,stored) {
  const [salt,hash]=String(stored).split(":");
  if(!salt||!hash)return false;
  const actual=Buffer.from(digest(password,salt).hash,"hex"),expected=Buffer.from(hash,"hex");
  return actual.length===expected.length&&crypto.timingSafeEqual(actual,expected);
}
export class SqliteIdentityProvider extends IdentityProvider {
  constructor(db,jwtSecret) { super(); this.db=db; this.jwtSecret=jwtSecret; }
  token(user) { return { accessToken: jwt.sign({sub:user.id,email:user.email},this.jwtSecret,{expiresIn:"30m",issuer:"balidan-rwa-api"}),tokenType:"Bearer" }; }
  register({name,email,password}) {
    const id=crypto.randomUUID(),{salt,hash}=digest(password);
    this.db.prepare("INSERT INTO users(id,name,email,password_hash,created_at) VALUES(?,?,?,?,?)").run(id,name.trim(),email.trim().toLowerCase(),salt+":"+hash,new Date().toISOString());
    return this.token({id,email:email.trim().toLowerCase()});
  }
  authenticate({email,password}) {
    const user=this.db.prepare("SELECT id,email,password_hash FROM users WHERE email=? COLLATE NOCASE").get(email.trim());
    if(!user||!verify(password,user.password_hash))return null;
    return this.token(user);
  }
}
export class SqliteEntitlementProvider extends EntitlementProvider {
  constructor(db) { super(); this.db=db; }
  async check(userId,contentId) {
    const row=this.db.prepare("SELECT decision,expires_at AS expiresAt FROM entitlement_checks WHERE user_id=? AND content_id=? ORDER BY checked_at DESC LIMIT 1").get(userId,contentId);
    if(!row || (row.expiresAt && Date.parse(row.expiresAt)<=Date.now()))return {contentId,decision:"unknown"};
    return {contentId,decision:row.decision};
  }
}
export class UnconfiguredPlaybackProvider extends PlaybackProvider {
  async createSession(_userId,contentId) {
    return {contentId,status:"unavailable",reason:"No authorized playback provider is configured."};
  }
}
