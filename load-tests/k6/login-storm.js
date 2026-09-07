import http from 'k6/http';
import {check} from 'k6';
import exec from 'k6/execution';
import {Rate} from 'k6/metrics';
import {SharedArray} from 'k6/data';

const baseUrl = (__ENV.BASE_URL || 'https://olifan.pixsom.fr').replace(/\/$/, '');
const usersFile = __ENV.USERS_FILE || './users.csv';

const users = new SharedArray('load-test users', () => parseUsers(open(usersFile)));
const vus = Number(__ENV.VUS || users.length);
const loginSuccess = new Rate('login_success');

if (vus > users.length) {
  throw new Error(`VUS=${vus}, mais ${users.length} utilisateur(s) seulement dans ${usersFile}`);
}

export const options = {
  scenarios: {
    login_storm: {
      executor: 'per-vu-iterations',
      vus,
      iterations: 1,
      maxDuration: '2m'
    }
  },
  thresholds: {
    login_success: ['rate>0.99']
  },
  discardResponseBodies: true
};

export default function () {
  const user = users[exec.vu.idInTest - 1];
  const response = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify(loginPayload(user)),
    {
      headers: {'Content-Type': 'application/json'},
      tags: {name: 'POST /api/auth/login'}
    }
  );

  const success = response.status === 200;
  loginSuccess.add(success);
  check(response, {'connexion acceptée': () => success});
}

function parseUsers(csv) {
  const lines = csv.replace(/^\uFEFF/, '').trim().split(/\r?\n/);
  if (lines.length < 2) throw new Error(`Le fichier ${usersFile} ne contient aucun utilisateur`);

  return lines.slice(1).filter(Boolean).map((line, index) => {
    const [email, , , accessCode = ''] = line.split(';').map((value) => value.trim());
    if (!email) throw new Error(`Email manquant à la ligne ${index + 2} de ${usersFile}`);
    return {email, accessCode};
  });
}

function loginPayload(user) {
  const accessCode = user.accessCode || __ENV.ACCESS_CODE || '';
  return accessCode ? {email: user.email, accessCode} : {email: user.email};
}
