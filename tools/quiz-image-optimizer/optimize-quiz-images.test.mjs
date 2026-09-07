import {randomBytes} from 'node:crypto';
import {mkdtemp, readFile, rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import path from 'node:path';
import test from 'node:test';
import assert from 'node:assert/strict';

import sharp from 'sharp';

import {optimizeImage, optimizeQuiz, readConfig, run} from './optimize-quiz-images.mjs';

const IMAGE_OPTIONS = {targetBytes: 300 * 1024, maxBytes: 500 * 1024, maxDimension: 1600};

test('compresses and resizes a large image to WebP', async () => {
  const width = 2200;
  const height = 1400;
  const png = await sharp(randomBytes(width * height * 3), {raw: {width, height, channels: 3}}).png().toBuffer();

  const result = await optimizeImage(dataUrl('image/png', png), IMAGE_OPTIONS);
  const output = Buffer.from(result.dataUrl.split(',')[1], 'base64');
  const metadata = await sharp(output).metadata();

  assert.match(result.dataUrl, /^data:image\/webp;base64,/);
  assert.ok(result.optimizedBytes <= IMAGE_OPTIONS.maxBytes);
  assert.ok(Math.max(metadata.width, metadata.height) <= IMAGE_OPTIONS.maxDimension);
});

test('supports images smaller than the resize floor', async () => {
  const png = await sharp({create: {width: 120, height: 80, channels: 4, background: '#ea580c'}}).png().toBuffer();
  const result = await optimizeImage(dataUrl('image/png', png), IMAGE_OPTIONS);

  assert.ok(result.optimizedBytes > 0);
  assert.equal(result.outputMaxDimension, 120);
});

test('rejects a content type that does not match the image bytes', async () => {
  const png = await sharp({create: {width: 10, height: 10, channels: 3, background: '#000'}}).png().toBuffer();

  await assert.rejects(() => optimizeImage(dataUrl('image/jpeg', png), IMAGE_OPTIONS), /format declare/);
});

test('rejects animated WebP images rather than silently dropping frames', async () => {
  const frames = [
    {create: {width: 10, height: 10, channels: 4, background: '#f00'}},
    {create: {width: 10, height: 10, channels: 4, background: '#00f'}}
  ];
  const animatedWebp = await sharp(frames, {join: {animated: true}})
    .webp({delay: [100, 100], loop: 0})
    .toBuffer();

  await assert.rejects(
    () => optimizeImage(dataUrl('image/webp', animatedWebp), IMAGE_OPTIONS),
    /animees ou multipages/
  );
});

test('builds a create request without carrying database identifiers', async () => {
  const png = await sharp({create: {width: 20, height: 20, channels: 3, background: '#fff'}}).png().toBuffer();
  const quiz = quizFixture(dataUrl('image/png', png));

  const result = await optimizeQuiz(quiz, {...IMAGE_OPTIONS, suffix: ' [Optimise]'});

  assert.equal(result.request.title, 'Quiz source [Optimise]');
  assert.equal(result.request.questions[0].answers[0].correct, true);
  assert.equal('id' in result.request.questions[0], false);
  assert.equal('id' in result.request.questions[0].answers[0], false);
  assert.equal(result.report.imageCount, 1);
});

test('defaults to dry-run and requires an explicit production confirmation for writes', () => {
  const base = {
    BASE_URL: 'https://olifan.pixsom.fr',
    QUIZ_IDS: '7,8,7',
    ADMIN_TOKEN: 'test-token'
  };

  assert.equal(readConfig(base).apply, false);
  assert.deepEqual(readConfig(base).quizIds, [7, 8]);
  assert.throws(() => readConfig({...base, APPLY: 'true'}), /PRODUCTION_CONFIRMATION/);
  assert.throws(
    () => readConfig({...base, BASE_URL: 'https://olifan.pixsom.fr.', APPLY: 'true'}),
    /PRODUCTION_CONFIRMATION/
  );
  assert.equal(readConfig({...base, APPLY: 'true', PRODUCTION_CONFIRMATION: 'DUPLIQUER_QUIZ_PROD'}).apply, true);
  assert.throws(() => readConfig({...base, MAX_KB: '5121'}), /MAX_KB/);
});

test('dry-run performs no POST and writes a report without image payloads', async () => {
  const directory = await mkdtemp(path.join(tmpdir(), 'quiz-optimizer-'));
  const png = await sharp({create: {width: 32, height: 32, channels: 3, background: '#123456'}}).png().toBuffer();
  const requests = [];
  const responses = [[], quizFixture(dataUrl('image/png', png))];
  const fetchMock = async (url, options) => {
    requests.push({url, method: options.method});
    return Response.json(responses.shift());
  };

  try {
    const results = await run({
      ...IMAGE_OPTIONS,
      baseUrl: 'https://example.test',
      apply: false,
      token: 'test-token',
      quizIds: [42],
      suffix: ' [Optimise]',
      maxRequestBytes: 45 * 1024 * 1024,
      maxResponseBytes: 10 * 1024 * 1024,
      reportDirectory: directory
    }, {fetch: fetchMock});

    assert.deepEqual(requests.map((request) => request.method), ['GET', 'GET']);
    assert.equal(results[0].status, 'simulated');
    const files = await import('node:fs/promises').then((fs) => fs.readdir(directory));
    const report = await readFile(path.join(directory, files[0]), 'utf8');
    assert.equal(report.includes('data:image/'), false);
  } finally {
    await rm(directory, {recursive: true, force: true});
  }
});

test('apply mode creates a new quiz and only sends the sanitized create contract', async () => {
  const directory = await mkdtemp(path.join(tmpdir(), 'quiz-optimizer-'));
  const png = await sharp({create: {width: 32, height: 32, channels: 3, background: '#654321'}}).png().toBuffer();
  const requests = [];
  const responses = [[], quizFixture(dataUrl('image/png', png)), {id: 84}];
  const fetchMock = async (url, options) => {
    requests.push({url, method: options.method, body: options.body});
    return Response.json(responses.shift());
  };

  try {
    const results = await run({
      ...IMAGE_OPTIONS,
      baseUrl: 'https://example.test',
      apply: true,
      token: 'test-token',
      quizIds: [42],
      suffix: ' [Optimise]',
      maxRequestBytes: 45 * 1024 * 1024,
      maxResponseBytes: 10 * 1024 * 1024,
      reportDirectory: directory
    }, {fetch: fetchMock});

    assert.deepEqual(requests.map((request) => request.method), ['GET', 'GET', 'POST']);
    const createRequest = JSON.parse(requests[2].body);
    assert.equal(createRequest.title, 'Quiz source [Optimise]');
    assert.equal('id' in createRequest.questions[0], false);
    assert.equal('id' in createRequest.questions[0].answers[0], false);
    assert.equal(results[0].createdQuizId, 84);
    assert.equal(results[0].status, 'created');
  } finally {
    await rm(directory, {recursive: true, force: true});
  }
});

test('dry-run detects title collisions caused by the maximum title length', async () => {
  const directory = await mkdtemp(path.join(tmpdir(), 'quiz-optimizer-'));
  const png = await sharp({create: {width: 16, height: 16, channels: 3, background: '#abcdef'}}).png().toBuffer();
  const commonTitle = 'A'.repeat(129);
  const responses = [
    [],
    {...quizFixture(dataUrl('image/png', png)), id: 41, title: `${commonTitle} premier`},
    {...quizFixture(dataUrl('image/png', png)), id: 42, title: `${commonTitle} second`}
  ];
  const requests = [];
  const fetchMock = async (url, options) => {
    requests.push(options.method);
    return Response.json(responses.shift());
  };

  try {
    await assert.rejects(() => run({
      ...IMAGE_OPTIONS,
      baseUrl: 'https://example.test',
      apply: false,
      token: 'test-token',
      quizIds: [41, 42],
      suffix: ' [Optimise]',
      maxRequestBytes: 45 * 1024 * 1024,
      maxResponseBytes: 10 * 1024 * 1024,
      reportDirectory: directory
    }, {fetch: fetchMock}), /existe deja/);

    assert.deepEqual(requests, ['GET', 'GET', 'GET']);
  } finally {
    await rm(directory, {recursive: true, force: true});
  }
});

function quizFixture(image) {
  return {
    id: 42,
    title: 'Quiz source',
    questions: [{
      id: 100,
      text: 'Question ?',
      questionImageDataUrl: image,
      answerImageDataUrl: null,
      durationSeconds: 30,
      answers: [
        {id: 1, text: 'Oui', correct: true},
        {id: 2, text: 'Non', correct: false}
      ]
    }]
  };
}

function dataUrl(contentType, bytes) {
  return `data:${contentType};base64,${bytes.toString('base64')}`;
}
