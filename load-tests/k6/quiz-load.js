import http from 'k6/http';
import {check, sleep} from 'k6';
import exec from 'k6/execution';
import {Rate, Trend} from 'k6/metrics';
import {SharedArray} from 'k6/data';

const baseUrl = (__ENV.BASE_URL || 'https://olifan.pixsom.fr').replace(/\/$/, '');
const usersFile = __ENV.USERS_FILE || './users.csv';
const users = new SharedArray('load-test users', () => parseUsers(open(usersFile)));
const vus = Number(__ENV.VUS || users.length);
const duration = __ENV.DURATION || '20m';
const loginIntervalMs = Number(__ENV.LOGIN_INTERVAL_MS || 300);
const answerDelayMs = Number(__ENV.ANSWER_DELAY_MS || 3000);

const snapshotDuration = new Trend('quiz_snapshot_duration', true);
const answerDuration = new Trend('quiz_answer_duration', true);
const imageDuration = new Trend('quiz_image_duration', true);
const answerAccepted = new Rate('quiz_answer_accepted');

let sessionId = null;
let joined = false;
let lastAnsweredQuestionId = null;
let lastQuestionImageUrl = null;
let lastAnswerImageUrl = null;
let nextSlowPollAt = 0;

if (vus > users.length) {
  throw new Error(`VUS=${vus}, mais ${users.length} utilisateur(s) seulement dans ${usersFile}`);
}

export const options = {
  setupTimeout: '2m',
  scenarios: {
    quiz: {
      executor: 'constant-vus',
      vus,
      duration,
      gracefulStop: '10s'
    }
  },
  thresholds: {
    quiz_snapshot_duration: ['p(95)<500', 'p(99)<1000'],
    quiz_answer_duration: ['p(95)<1000', 'p(99)<2000'],
    quiz_answer_accepted: ['rate>0.99']
  }
};

export function setup() {
  const tokens = [];

  for (let index = 0; index < vus; index++) {
    const response = http.post(
      `${baseUrl}/api/auth/login`,
      JSON.stringify(loginPayload(users[index])),
      {
        headers: {'Content-Type': 'application/json'},
        tags: {name: 'SETUP POST /api/auth/login'}
      }
    );

    if (response.status !== 200) {
      throw new Error(`Connexion impossible pour ${users[index].email}: HTTP ${response.status}`);
    }

    const token = response.json('token');
    if (!token) throw new Error(`JWT absent pour ${users[index].email}`);
    tokens.push(token);
    sleep(loginIntervalMs / 1000);
  }

  return {tokens};
}

export default function (data) {
  const iterationStartedAt = Date.now();
  const token = data.tokens[exec.vu.idInTest - 1];
  const authParams = {
    headers: {Authorization: `Bearer ${token}`}
  };

  const requests = [
    namedGet('/api/quizzes/live', authParams, http.expectedStatuses(200))
  ];

  let snapshotIndex = -1;
  if (sessionId !== null) {
    snapshotIndex = requests.length;
    requests.push(namedGet(`/api/quizzes/sessions/${sessionId}`, authParams, http.expectedStatuses(200)));
  }

  if (Date.now() >= nextSlowPollAt) {
    requests.push(namedGet('/api/app/branding', authParams, http.expectedStatuses(200)));
    requests.push(namedGet('/api/app/features', authParams, http.expectedStatuses(200)));
    nextSlowPollAt = Date.now() + 2000;
  }

  const responses = http.batch(requests);
  const liveResponse = responses[0];

  if (sessionId === null && liveResponse.status === 200) {
    const sessions = liveResponse.json();
    if (Array.isArray(sessions) && sessions.length > 0) sessionId = sessions[0].id;
  }

  let snapshot = null;
  if (snapshotIndex >= 0) {
    const snapshotResponse = responses[snapshotIndex];
    snapshotDuration.add(snapshotResponse.timings.duration);
    if (snapshotResponse.status === 200) snapshot = snapshotResponse.json();
  }

  if (sessionId !== null && snapshot !== null && !joined) {
    const joinResponse = http.post(
      `${baseUrl}/api/quizzes/sessions/${sessionId}/join`,
      null,
      withName(authParams, 'POST /api/quizzes/sessions/:id/join')
    );
    check(joinResponse, {'session rejointe': (response) => response.status === 200});
    if (joinResponse.status === 200) {
      joined = true;
      snapshot = joinResponse.json();
    }
  }

  if (joined && snapshot?.phase === 'QUESTION_OPEN' && snapshot.currentQuestion) {
    answerCurrentQuestion(snapshot, token);
  }
  if (snapshot?.currentQuestion) loadNewImages(snapshot.currentQuestion, authParams);

  const remainingMs = 1000 - (Date.now() - iterationStartedAt);
  if (remainingMs > 0) sleep(remainingMs / 1000);
}

function loadNewImages(question, authParams) {
  for (const [url, previousUrl, remember] of [
    [question.questionImageUrl, lastQuestionImageUrl, (value) => (lastQuestionImageUrl = value)],
    [question.answerImageUrl, lastAnswerImageUrl, (value) => (lastAnswerImageUrl = value)]
  ]) {
    if (!url || url === previousUrl) continue;
    const response = http.get(`${baseUrl}${url}`, withName(authParams, 'GET /api/quizzes/sessions/:id/questions/:id/images/:kind'));
    imageDuration.add(response.timings.duration);
    check(response, {'image quiz chargée': (result) => result.status === 200});
    remember(url);
  }
}

function answerCurrentQuestion(snapshot, token) {
  const question = snapshot.currentQuestion;
  if (question.id === lastAnsweredQuestionId || snapshot.selectedAnswerId !== null) return;

  const targetTime = Date.parse(snapshot.phaseStartedAt) + answerDelayMs;
  const waitMs = targetTime - Date.now();
  if (waitMs > 0) sleep(waitMs / 1000);

  const answers = question.answers || [];
  if (answers.length === 0) return;

  const answerId = answers[(exec.vu.idInTest - 1) % answers.length].id;
  const response = http.post(
    `${baseUrl}/api/quizzes/sessions/${sessionId}/answers`,
    JSON.stringify({answerId}),
    {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      tags: {name: 'POST /api/quizzes/sessions/:id/answers'}
    }
  );

  answerDuration.add(response.timings.duration);
  let accepted = false;
  if (response.status === 200) {
    accepted = response.json('selectedAnswerId') === answerId;
  }
  answerAccepted.add(accepted);
  check(response, {'réponse acceptée': () => accepted});
  lastAnsweredQuestionId = question.id;
}

function namedGet(path, baseParams, responseCallback) {
  return {
    method: 'GET',
    url: `${baseUrl}${path}`,
    params: {
      ...baseParams,
      responseCallback,
      tags: {name: `GET ${path.replace(/\/sessions\/\d+/, '/sessions/:id')}`}
    }
  };
}

function withName(baseParams, name) {
  return {...baseParams, tags: {name}};
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
