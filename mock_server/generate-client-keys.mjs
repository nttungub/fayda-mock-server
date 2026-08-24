// Generates an RSA keypair for a test relying party (your app).
// Requires: npm install jose
//
// Usage:
//   node generate-client-keys.mjs
//
// The PUBLIC_JWK goes into the "publicJwk" field when you POST to /admin/clients.
// The PRIVATE_KEY_BASE64 is what your relying party app uses to sign client_assertion JWTs
// (same pattern as PRIVATE_KEY_BASE64 in the real Fayda reference apps).

import { generateKeyPair, exportJWK } from 'jose';

const { publicKey, privateKey } = await generateKeyPair('RS256', { extractable: true });

const publicJwk = await exportJWK(publicKey);
const privateJwk = await exportJWK(privateKey);

const kid = 'mock-client-key-1';
publicJwk.use = 'sig'; publicJwk.alg = 'RS256'; publicJwk.kid = kid;
privateJwk.use = 'sig'; privateJwk.alg = 'RS256'; privateJwk.kid = kid;

console.log('=== PUBLIC_JWK (put this in "publicJwk" when POSTing to /admin/clients) ===');
console.log(JSON.stringify(publicJwk, null, 2));

console.log('\n=== PRIVATE_KEY_BASE64 (use this in your relying party app to sign client_assertion) ===');
console.log(Buffer.from(JSON.stringify(privateJwk)).toString('base64'));
