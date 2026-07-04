export interface AuthUser {
  email: string;
  fullName: string;
  roles: string[];
}

export interface AuthSession {
  accessToken: string;
  expiresIn: number;
  user: AuthUser;
}
