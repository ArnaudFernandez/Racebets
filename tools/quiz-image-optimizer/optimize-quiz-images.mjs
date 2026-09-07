import {mkdir, writeFile} from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import {pathToFileURL} from 'node:url';

import sharp from 'sharp';

const ALLOWED_IMAGE_TYPES = new Map([
  ['image/jpeg', 'jpeg'],
  ['image/png', 'png'],
  ['image/webp', 'webp']
]);
const QUALITY_FLOOR = 50;
const QUALITY_CEILING = 82;
const MAX_INPUT_PIXELS = 40_000_000;
const PRODUCTION_CONFIRMATION = 'DUPLIQUER_QUIZ_PROD';

export function readConfig(environment = process.env) {
  const baseUrl = required(environment.BASE_URL, 'BASE_URL').replace(/\/$/, '');
  const parsedUrl = new URL(baseUrl);
  if (parsedUrl.protocol !== 'https:' && !isLocalHost(parsedUrl.hostname) && environment.ALLOW_HTTP !== 'true') {
    throw new Error('BASE_URL doit utiliser HTTPS hors environnement local');
  }
  if (parsedUrl.username || parsedUrl.password || parsedUrl.search || parsedUrl.hash) {
    throw new Error('BASE_URL ne doit contenir ni identifiants, ni query string, ni fragment');
  }

  const apply = environment.APPLY === 'true';
  const canonicalHostname = parsedUrl.hostname.toLowerCase().replace(/\.$/, '');
  if (apply && canonicalHostname === 'olifan.pixsom.fr'
      && environment.PRODUCTION_CONFIRMATION !== PRODUCTION_CONFIRMATION) {
    throw new Error(`Pour ecrire en production, definir PRODUCTION_CONFIRMATION=${PRODUCTION_CONFIRMATION}`);
  }

  const token = environment.ADMIN_TOKEN?.trim() || null;
  const email = environment.ADMIN_EMAIL?.trim() || null;
  const accessCode = environment.ADMIN_ACCESS_CODE || null;
  if (token === null && (email === null || accessCode === null)) {
    throw new Error('Definir ADMIN_TOKEN ou ADMIN_EMAIL et ADMIN_ACCESS_CODE');
  }

  const targetBytes = boundedInteger(environment.TARGET_KB ?? '300', 'TARGET_KB', 5120) * 1024;
  const maxBytes = boundedInteger(environment.MAX_KB ?? '500', 'MAX_KB', 5120) * 1024;
  if (targetBytes > maxBytes) throw new Error('TARGET_KB doit etre inferieur ou egal a MAX_KB');

  return {
    baseUrl,
    apply,
    token,
    email,
    accessCode,
    quizIds: parseQuizIds(required(environment.QUIZ_IDS, 'QUIZ_IDS')),
    suffix: environment.COPY_SUFFIX ?? ' [Optimise]',
    targetBytes,
    maxBytes,
    maxDimension: boundedInteger(environment.MAX_DIMENSION ?? '1600', 'MAX_DIMENSION', 8192),
    maxRequestBytes: boundedInteger(environment.MAX_REQUEST_MB ?? '45', 'MAX_REQUEST_MB', 49) * 1024 * 1024,
    maxResponseBytes: boundedInteger(environment.MAX_RESPONSE_MB ?? '250', 'MAX_RESPONSE_MB', 1024) * 1024 * 1024,
    reportDirectory: path.resolve(environment.REPORT_DIR ?? 'reports')
  };
}

export async function optimizeImage(dataUrl, options) {
  const source = parseDataUrl(dataUrl);
  const image = sharp(source.bytes, {
    failOn: 'warning',
    limitInputPixels: MAX_INPUT_PIXELS,
    sequentialRead: true
  });
  const metadata = await image.metadata();
  const expectedFormat = ALLOWED_IMAGE_TYPES.get(source.contentType);
  if ((metadata.pages ?? 1) !== 1) {
    throw new Error('Les images animees ou multipages ne sont pas prises en charge');
  }
  if (metadata.format !== expectedFormat || !metadata.width || !metadata.height) {
    throw new Error(`Le contenu ${source.contentType} ne correspond pas a son format declare`);
  }

  const orientedWidth = swapsDimensions(metadata.orientation) ? metadata.height : metadata.width;
  const orientedHeight = swapsDimensions(metadata.orientation) ? metadata.width : metadata.height;
  let dimension = Math.min(options.maxDimension, Math.max(orientedWidth, orientedHeight));
  const minimumDimension = Math.min(640, dimension);
  let smallest = null;

  while (dimension >= minimumDimension) {
    const highQuality = await encodeWebp(source.bytes, dimension, QUALITY_CEILING);
    smallest = smaller(smallest, {bytes: highQuality, dimension, quality: QUALITY_CEILING});
    if (highQuality.length <= options.targetBytes) return imageResult(source, highQuality, metadata, dimension, QUALITY_CEILING);

    const lowQuality = await encodeWebp(source.bytes, dimension, QUALITY_FLOOR);
    smallest = smaller(smallest, {bytes: lowQuality, dimension, quality: QUALITY_FLOOR});
    if (lowQuality.length <= options.targetBytes) {
      let lower = QUALITY_FLOOR;
      let upper = QUALITY_CEILING;
      let selected = lowQuality;
      while (upper - lower > 2) {
        const quality = Math.floor((lower + upper) / 2);
        const candidate = await encodeWebp(source.bytes, dimension, quality);
        if (candidate.length <= options.targetBytes) {
          lower = quality;
          selected = candidate;
        } else {
          upper = quality;
        }
      }
      return imageResult(source, selected, metadata, dimension, lower);
    }

    if (dimension === minimumDimension) break;
    const nextDimension = Math.max(minimumDimension, Math.floor(dimension * 0.85));
    if (nextDimension === dimension) break;
    dimension = nextDimension;
  }

  if (smallest !== null && smallest.bytes.length <= options.maxBytes) {
    return imageResult(source, smallest.bytes, metadata, smallest.dimension, smallest.quality);
  }
  throw new Error(`Impossible de compresser l'image sous ${formatBytes(options.maxBytes)}`);
}

export async function optimizeQuiz(quiz, options) {
  validateQuiz(quiz);
  const questions = [];
  const images = [];

  for (const [questionIndex, question] of quiz.questions.entries()) {
    const optimizedQuestionImage = await optimizeOptionalImage(
      question.questionImageDataUrl,
      options,
      questionIndex,
      'question',
      images
    );
    const optimizedAnswerImage = await optimizeOptionalImage(
      question.answerImageDataUrl,
      options,
      questionIndex,
      'answer',
      images
    );
    questions.push({
      text: question.text,
      questionImageDataUrl: optimizedQuestionImage,
      answerImageDataUrl: optimizedAnswerImage,
      durationSeconds: question.durationSeconds,
      answers: question.answers.map((answer) => ({text: answer.text, correct: answer.correct}))
    });
  }

  if (images.length === 0) throw new Error(`Le quiz ${quiz.id} ne contient aucune image a optimiser`);
  const title = suffixedTitle(quiz.title, options.suffix);
  return {
    request: {title, questions},
    report: {
      sourceQuizId: quiz.id,
      sourceTitle: quiz.title,
      targetTitle: title,
      imageCount: images.length,
      sourceBytes: images.reduce((total, image) => total + image.sourceBytes, 0),
      optimizedBytes: images.reduce((total, image) => total + image.optimizedBytes, 0),
      images
    }
  };
}

export async function run(config, dependencies = {}) {
  const fetchImplementation = dependencies.fetch ?? globalThis.fetch;
  if (typeof fetchImplementation !== 'function') throw new Error('Cette version de Node ne fournit pas fetch');

  const token = config.token ?? await login(config, fetchImplementation);
  const headers = {Authorization: `Bearer ${token}`};
  const quizList = await requestJson(fetchImplementation, `${config.baseUrl}/api/admin/quizzes`, {
    headers,
    maxResponseBytes: config.maxResponseBytes
  });
  if (!Array.isArray(quizList)) throw new Error('La liste des quiz retournee par le serveur est invalide');
  const existingTitles = new Set(quizList.map((quiz) => quiz.title));
  const results = [];
  const reportPath = await createReport(config, results);

  for (const quizId of config.quizIds) {
    const quiz = await requestJson(fetchImplementation, `${config.baseUrl}/api/admin/quizzes/${quizId}`, {
      headers,
      maxResponseBytes: config.maxResponseBytes
    });
    const optimized = await optimizeQuiz(quiz, config);
    if (existingTitles.has(optimized.request.title)) {
      throw new Error(`Un quiz nomme "${optimized.request.title}" existe deja; aucune copie n'a ete creee`);
    }

    const requestBody = JSON.stringify(optimized.request);
    const requestBytes = Buffer.byteLength(requestBody);
    if (requestBytes > config.maxRequestBytes) {
      throw new Error(`Le quiz ${quizId} produit une requete de ${formatBytes(requestBytes)}, au-dessus de la limite de securite`);
    }

    const result = {
      ...optimized.report,
      requestBytes,
      createdQuizId: null,
      status: config.apply ? 'creation-pending' : 'simulated'
    };
    results.push(result);
    await updateReport(config, results, reportPath);

    if (config.apply) {
      try {
        const created = await requestJson(fetchImplementation, `${config.baseUrl}/api/admin/quizzes`, {
          method: 'POST',
          headers: {...headers, 'Content-Type': 'application/json'},
          body: requestBody,
          maxResponseBytes: config.maxResponseBytes
        });
        if (!Number.isSafeInteger(created?.id) || created.id < 1) {
          throw new Error('La creation a retourne un identifiant de quiz invalide');
        }
        result.createdQuizId = created.id;
        result.status = 'created';
        existingTitles.add(optimized.request.title);
      } catch (error) {
        result.status = 'creation-indeterminate';
        await updateReport(config, results, reportPath);
        throw error;
      }
    } else {
      existingTitles.add(optimized.request.title);
    }
    await updateReport(config, results, reportPath);
    printResult(result, config.apply);
  }
  return results;
}

async function login(config, fetchImplementation) {
  const response = await requestJson(fetchImplementation, `${config.baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({email: config.email, accessCode: config.accessCode}),
    maxResponseBytes: 1024 * 1024
  });
  if (typeof response.token !== 'string' || response.token.length === 0) {
    throw new Error('La connexion administrateur n\'a retourne aucun JWT');
  }
  return response.token;
}

async function requestJson(fetchImplementation, url, options) {
  const response = await fetchImplementation(url, {
    method: options.method ?? 'GET',
    headers: {...options.headers, Accept: 'application/json'},
    body: options.body,
    redirect: 'error',
    signal: AbortSignal.timeout(120_000)
  });
  const text = await readLimitedBody(response, options.maxResponseBytes);
  if (!response.ok) {
    const detail = text.slice(0, 300).replace(/\s+/g, ' ');
    throw new Error(`HTTP ${response.status} sur ${new URL(url).pathname}${detail ? `: ${detail}` : ''}`);
  }
  try {
    return JSON.parse(text);
  } catch {
    throw new Error(`Reponse JSON invalide sur ${new URL(url).pathname}`);
  }
}

async function readLimitedBody(response, maxBytes) {
  const declaredLength = Number(response.headers.get('content-length'));
  if (Number.isFinite(declaredLength) && declaredLength > maxBytes) {
    throw new Error(`Reponse refusee: ${formatBytes(declaredLength)} depasse la limite configuree`);
  }
  if (response.body === null) return '';

  const reader = response.body.getReader();
  const chunks = [];
  let total = 0;
  while (true) {
    const {done, value} = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > maxBytes) {
      await reader.cancel();
      throw new Error(`Reponse refusee: plus de ${formatBytes(maxBytes)}`);
    }
    chunks.push(Buffer.from(value));
  }
  return Buffer.concat(chunks, total).toString('utf8');
}

function parseDataUrl(dataUrl) {
  if (typeof dataUrl !== 'string') throw new Error('Image Data URL invalide');
  const match = /^data:(image\/(?:jpeg|png|webp));base64,([A-Za-z0-9+/]+={0,2})$/.exec(dataUrl.trim());
  if (match === null || match[2].length % 4 !== 0) throw new Error('Image Data URL invalide');
  const bytes = Buffer.from(match[2], 'base64');
  if (bytes.length === 0) throw new Error('Image vide');
  const canonical = bytes.toString('base64');
  if (canonical !== match[2]) throw new Error('Encodage Base64 non canonique');
  return {contentType: match[1], bytes};
}

async function optimizeOptionalImage(dataUrl, options, questionIndex, kind, reports) {
  if (dataUrl === null || dataUrl === undefined || dataUrl === '') return null;
  const result = await optimizeImage(dataUrl, options);
  reports.push({
    question: questionIndex + 1,
    kind,
    sourceType: result.sourceType,
    sourceWidth: result.sourceWidth,
    sourceHeight: result.sourceHeight,
    sourceBytes: result.sourceBytes,
    optimizedBytes: result.optimizedBytes,
    outputQuality: result.outputQuality,
    outputMaxDimension: result.outputMaxDimension,
    reductionPercent: Math.round((1 - result.optimizedBytes / result.sourceBytes) * 1000) / 10
  });
  return result.dataUrl;
}

async function encodeWebp(bytes, dimension, quality) {
  return sharp(bytes, {failOn: 'warning', limitInputPixels: MAX_INPUT_PIXELS, sequentialRead: true})
    .rotate()
    .resize({width: dimension, height: dimension, fit: 'inside', withoutEnlargement: true})
    .webp({quality, effort: 6, smartSubsample: true})
    .toBuffer();
}

function imageResult(source, bytes, metadata, dimension, quality) {
  return {
    dataUrl: `data:image/webp;base64,${bytes.toString('base64')}`,
    sourceType: source.contentType,
    sourceWidth: metadata.width,
    sourceHeight: metadata.height,
    sourceBytes: source.bytes.length,
    optimizedBytes: bytes.length,
    outputMaxDimension: dimension,
    outputQuality: quality
  };
}

function validateQuiz(quiz) {
  if (!Number.isSafeInteger(quiz?.id) || typeof quiz.title !== 'string' || !Array.isArray(quiz.questions)) {
    throw new Error('Le detail du quiz retourne par le serveur est invalide');
  }
  for (const [index, question] of quiz.questions.entries()) {
    if (typeof question.text !== 'string' || !Number.isInteger(question.durationSeconds)
        || !Array.isArray(question.answers) || question.answers.length < 2) {
      throw new Error(`La question ${index + 1} est invalide`);
    }
    if (question.answers.some((answer) => typeof answer.text !== 'string' || typeof answer.correct !== 'boolean')) {
      throw new Error(`Les reponses de la question ${index + 1} sont invalides`);
    }
    if (question.answers.filter((answer) => answer.correct).length !== 1) {
      throw new Error(`La question ${index + 1} doit avoir exactement une bonne reponse`);
    }
  }
}

function suffixedTitle(title, suffix) {
  if (typeof suffix !== 'string' || suffix.length === 0 || suffix.length >= 140) {
    throw new Error('COPY_SUFFIX doit contenir entre 1 et 139 caracteres');
  }
  if (title.endsWith(suffix)) throw new Error(`Le quiz "${title}" semble deja optimise`);
  return title.slice(0, 140 - suffix.length).trimEnd() + suffix;
}

async function createReport(config, results) {
  await mkdir(config.reportDirectory, {recursive: true});
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const reportPath = path.join(config.reportDirectory, `quiz-image-optimization-${timestamp}.json`);
  await updateReport(config, results, reportPath, 'wx');
  console.log(`Rapport: ${reportPath}`);
  return reportPath;
}

async function updateReport(config, results, reportPath, flag = 'w') {
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl: config.baseUrl,
    mode: config.apply ? 'apply' : 'dry-run',
    results
  };
  await writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`, {encoding: 'utf8', flag});
}

function printResult(result, applied) {
  const reduction = Math.round((1 - result.optimizedBytes / result.sourceBytes) * 1000) / 10;
  console.log(`${applied ? 'Cree' : 'Simulation'}: ${result.targetTitle}`);
  console.log(`  ${result.imageCount} image(s): ${formatBytes(result.sourceBytes)} -> ${formatBytes(result.optimizedBytes)} (-${reduction}%)`);
  if (result.createdQuizId !== null) console.log(`  Nouveau quiz: ${result.createdQuizId}`);
}

function parseQuizIds(value) {
  const ids = value.split(',').map((item) => positiveInteger(item.trim(), 'QUIZ_IDS'));
  if (ids.length === 0) throw new Error('QUIZ_IDS ne contient aucun identifiant');
  return [...new Set(ids)];
}

function positiveInteger(value, name) {
  if (!/^\d+$/.test(value)) throw new Error(`${name} doit etre un entier strictement positif`);
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 1) throw new Error(`${name} doit etre un entier strictement positif`);
  return parsed;
}

function boundedInteger(value, name, maximum) {
  const parsed = positiveInteger(value, name);
  if (parsed > maximum) throw new Error(`${name} doit etre inferieur ou egal a ${maximum}`);
  return parsed;
}

function required(value, name) {
  if (typeof value !== 'string' || value.trim() === '') throw new Error(`${name} est obligatoire`);
  return value.trim();
}

function isLocalHost(hostname) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1';
}

function swapsDimensions(orientation) {
  return orientation === 5 || orientation === 6 || orientation === 7 || orientation === 8;
}

function smaller(current, candidate) {
  return current === null || candidate.bytes.length < current.bytes.length ? candidate : current;
}

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
  return `${(bytes / 1024 / 1024).toFixed(2)} Mo`;
}

async function main() {
  try {
    const config = readConfig();
    console.log(`Mode: ${config.apply ? 'CREATION DES COPIES' : 'SIMULATION SANS CREATION'}`);
    console.log(`Serveur: ${config.baseUrl}`);
    console.log(`Quiz: ${config.quizIds.join(', ')}`);
    await run(config);
  } catch (error) {
    console.error(`Echec: ${error instanceof Error ? error.message : String(error)}`);
    process.exitCode = 1;
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  await main();
}
