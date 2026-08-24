export type TutorialTarget = 'none' | 'stage' | 'market' | 'runner' | 'bet' | 'result' | 'history';
export type TutorialAction = 'continue' | 'choose-runner' | 'finish';

export interface TutorialStep {
  readonly id: string;
  readonly eyebrow: string;
  readonly title: string;
  readonly description: string;
  readonly target: TutorialTarget;
  readonly action: TutorialAction;
  readonly actionLabel?: string;
}

export interface TutorialRunner {
  readonly entryId: number;
  readonly horseName: string;
  readonly horseNumber: number;
  readonly rank: number;
}

export interface TutorialHistoryEntry {
  readonly raceName: string;
  readonly selectedHorseName: string;
  readonly winningHorseName: string;
  readonly outcome: 'WON' | 'LOST';
  readonly speedRank: number | null;
}

export const TUTORIAL_RUNNERS: readonly TutorialRunner[] = [
  { entryId: 101, horseName: 'Belle Allure', horseNumber: 3, rank: 2 },
  { entryId: 102, horseName: 'Éclair du Médoc', horseNumber: 7, rank: 1 },
  { entryId: 103, horseName: 'Vent d’Ouest', horseNumber: 11, rank: 3 },
  { entryId: 104, horseName: 'Rive Gauche', horseNumber: 14, rank: 4 }
];

export const TUTORIAL_WINNER_ID = 102;

export const TUTORIAL_HISTORY: readonly TutorialHistoryEntry[] = [
  {
    raceName: 'Prix de la Découverte',
    selectedHorseName: 'Éclair du Médoc',
    winningHorseName: 'Éclair du Médoc',
    outcome: 'WON',
    speedRank: 2
  },
  {
    raceName: 'Exemple d’un pari perdant',
    selectedHorseName: 'Belle Allure',
    winningHorseName: 'Vent d’Ouest',
    outcome: 'LOST',
    speedRank: null
  }
];

export const TUTORIAL_STEPS: readonly TutorialStep[] = [
  {
    id: 'welcome',
    eyebrow: 'Bienvenue',
    title: 'Votre première course',
    description: 'Voyons rapidement comment fonctionne les paris.\n(Vous pouvez quitter le tutoriel à tout moment en haut à droite de votre écran)',
    target: 'none',
    action: 'continue',
    actionLabel: 'Commencer'
  },
  {
    id: 'waiting',
    eyebrow: 'Avant la course',
    title: 'Patientez sur l’écran d’accueil',
    description: 'Quand aucune course n’est ouverte, restez simplement ici. L’écran se met à jour automatiquement dès que l’organisateur prépare une course.',
    target: 'stage',
    action: 'continue',
    actionLabel: 'Suivant'
  },
  {
    id: 'betting-open',
    eyebrow: 'Paris ouverts',
    title: 'Pariez au bon moment !',
    description: 'Vous pouvez agir uniquement lorsque « Votes ouverts » est affiché. Vous serez avertis juste avant que les paris soient ouverts.',
    target: 'market',
    action: 'continue',
    actionLabel: 'Choisir un cheval'
  },
  {
    id: 'choose',
    eyebrow: 'À vous de jouer',
    title: 'Pariez sur Éclair du Médoc',
    description: 'Touchez Éclair du Médoc. Le pari est enregistré immédiatement dans une vraie course. Ici, il reste entièrement fictif.',
    target: 'runner',
    action: 'choose-runner'
  },
  {
    id: 'bet-placed',
    eyebrow: 'Pari enregistré',
    title: 'Votre sélection est confirmée',
    description: 'La fiche verte et l’heure du vote confirment votre choix. Changer de cheval reste possible tant que les votes sont ouverts, mais réinitialise votre heure de vote.',
    target: 'bet',
    action: 'continue',
    actionLabel: 'La course commence !'
  },
  {
    id: 'race-running',
    eyebrow: 'Course en cours',
    title: 'Maintenant, vous n’avez plus rien à faire',
    description: 'Les paris sont clos. Vous n\'avez qu\'à profiter de la course !',
    target: 'stage',
    action: 'continue',
    actionLabel: 'Une fois la course terminée ?'
  },
  {
    id: 'won',
    eyebrow: 'Résultat officiel',
    title: 'Vous avez gagné cette course de test !',
    description: 'Votre cheval est arrivé premier. Votre rang indique aussi votre rapidité parmi les personnes ayant choisi le même gagnant.',
    target: 'result',
    action: 'continue',
    actionLabel: 'Consulter l’historique'
  },
  {
    id: 'history',
    eyebrow: 'Après la course',
    title: 'Vos précédents sont consultables dans l’historique',
    description: 'Chaque course terminée entrera dans votre historique.',
    target: 'history',
    action: 'finish',
    actionLabel: 'Terminer le tutoriel'
  }
];
