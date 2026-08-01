export interface Env {
  DB: D1Database;
  MEDIA: KVNamespace;
  FIREBASE_PROJECT_ID: string;
}

export interface FirebaseUser {
  uid: string;
  email?: string;
  name?: string;
}

export type AppContext = {
  Bindings: Env;
  Variables: { firebaseUser: FirebaseUser };
};
