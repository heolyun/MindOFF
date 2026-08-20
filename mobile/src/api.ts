import { Platform } from 'react-native';

import type {
  AttentionItem,
  FridgeItem,
  HomeSummary,
  HouseholdDetails,
  HouseholdInvitation,
  HouseholdItem,
  HouseholdMember,
  NeedListItem,
  ReceiptItemTarget,
  ReceiptLine,
  ReceiptView,
  Session,
  Subscription,
} from './types';

const platformDefault = Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
export const IS_DEMO_MODE = process.env.EXPO_PUBLIC_DEMO_MODE === 'true';
const IS_COGNITO_MODE = process.env.EXPO_PUBLIC_AUTH_MODE === 'cognito';
let accessToken: string | null = null;
let accessTokenRefresher: (() => Promise<string | null>) | null = null;
export const API_BASE_URL = IS_DEMO_MODE
  ? 'Vercel preview data'
  : (process.env.EXPO_PUBLIC_API_URL ?? platformDefault).replace(/\/$/, '');

type SubscriptionInput = {
  name: string;
  amount: number;
  nextBillingAt: string | null;
  trialEndAt: string | null;
  managementUrl: string;
};

export type ReceiptUpload = {
  uri: string;
  fileName: string;
  mimeType: string;
  file?: Blob;
};

export type ReceiptConfirmInput = {
  merchantName: string;
  purchasedAt: string;
  totalAmount: number;
  lines: Array<{
    name: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
    targetType: ReceiptItemTarget;
    expiresAt: string | null;
  }>;
};

export type MindoffApi = {
  bootstrap(): Promise<Session>;
  getHome(session: Session): Promise<HomeSummary>;
  getHousehold(session: Session): Promise<HouseholdDetails>;
  getHouseholdInvitations(session: Session): Promise<HouseholdInvitation[]>;
  createHouseholdInvitation(session: Session, email: string): Promise<HouseholdInvitation>;
  getFridge(session: Session): Promise<FridgeItem[]>;
  addFridge(session: Session, name: string, expiresAt: string | null): Promise<FridgeItem>;
  finishFridge(session: Session, itemId: string): Promise<FridgeItem>;
  getHouseholdItems(session: Session): Promise<HouseholdItem[]>;
  addHouseholdItem(session: Session, name: string, purchaseUrl: string): Promise<HouseholdItem>;
  finishHouseholdItem(session: Session, itemId: string): Promise<HouseholdItem>;
  getNeeds(session: Session): Promise<NeedListItem[]>;
  addNeed(session: Session, name: string, purchaseUrl: string): Promise<NeedListItem>;
  completeNeed(session: Session, itemId: string): Promise<NeedListItem>;
  getSubscriptions(session: Session): Promise<Subscription[]>;
  addSubscription(session: Session, input: SubscriptionInput): Promise<Subscription>;
  getReceipts(session: Session): Promise<ReceiptView[]>;
  intakeReceipt(session: Session, upload: ReceiptUpload): Promise<ReceiptView>;
  confirmReceipt(session: Session, receiptId: string, input: ReceiptConfirmInput): Promise<ReceiptView>;
  getAttention(session: Session): Promise<AttentionItem[]>;
};

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await authenticatedFetch(path, options, true);

  if (!response.ok) {
    const body = await response.json().catch(() => undefined);
    const message = body?.message ?? body?.detail ?? `API 요청 실패 (${response.status})`;
    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

async function requestForm<T>(path: string, form: FormData): Promise<T> {
  const response = await authenticatedFetch(path, {
    method: 'POST',
    body: form,
  }, false);
  if (!response.ok) {
    const body = await response.json().catch(() => undefined);
    throw new Error(body?.message ?? body?.detail ?? `API 요청 실패 (${response.status})`);
  }
  return response.json() as Promise<T>;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function setAccessTokenRefresher(refresher: (() => Promise<string | null>) | null) {
  accessTokenRefresher = refresher;
}

async function authenticatedFetch(path: string, options: RequestInit | undefined, json: boolean): Promise<Response> {
  const send = () => fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(json ? { 'Content-Type': 'application/json' } : {}),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...options?.headers,
    },
  });
  let response = await send();
  if (response.status === 401 && accessTokenRefresher) {
    const refreshed = await accessTokenRefresher();
    if (refreshed) {
      accessToken = refreshed;
      response = await send();
    }
  }
  return response;
}

const remoteApi: MindoffApi = {
  bootstrap() {
    if (IS_COGNITO_MODE) {
      return request('/api/auth/session', { method: 'POST' });
    }
    return request('/api/dev/bootstrap', {
      method: 'POST',
      body: JSON.stringify({
        email: 'demo@mindoff.local',
        name: 'MindOFF Demo',
        householdName: '우리 집',
      }),
    });
  },

  getHome(session) {
    return request(`/api/home?householdId=${session.householdId}&userId=${session.userId}`);
  },

  getHousehold(session) {
    return request(`/api/households/${session.householdId}?userId=${session.userId}`);
  },

  getHouseholdInvitations(session) {
    return request(`/api/households/${session.householdId}/invitations?userId=${session.userId}`);
  },

  createHouseholdInvitation(session, email) {
    return request(`/api/households/${session.householdId}/invitations`, {
      method: 'POST',
      body: JSON.stringify({ requesterId: session.userId, email }),
    });
  },

  getFridge(session) {
    return request(`/api/households/${session.householdId}/fridge?userId=${session.userId}`);
  },

  addFridge(session, name, expiresAt) {
    return request(`/api/households/${session.householdId}/fridge`, {
      method: 'POST',
      body: JSON.stringify({ userId: session.userId, name, purchasedAt: today(), expiresAt }),
    });
  },

  finishFridge(session, itemId) {
    return request(`/api/fridge/${itemId}/finish`, {
      method: 'PATCH',
      body: JSON.stringify({ userId: session.userId, addToNeedList: true }),
    });
  },

  getHouseholdItems(session) {
    return request(`/api/households/${session.householdId}/items?userId=${session.userId}`);
  },

  addHouseholdItem(session, name, purchaseUrl) {
    return request(`/api/households/${session.householdId}/items`, {
      method: 'POST',
      body: JSON.stringify({
        userId: session.userId,
        name,
        purchasedAt: today(),
        repeatPurchase: true,
        purchaseUrl: purchaseUrl || null,
      }),
    });
  },

  finishHouseholdItem(session, itemId) {
    return request(`/api/household-items/${itemId}/finish`, {
      method: 'PATCH',
      body: JSON.stringify({ userId: session.userId, addToNeedList: true }),
    });
  },

  getNeeds(session) {
    return request(`/api/households/${session.householdId}/needs?userId=${session.userId}`);
  },

  addNeed(session, name, purchaseUrl) {
    return request(`/api/households/${session.householdId}/needs`, {
      method: 'POST',
      body: JSON.stringify({ userId: session.userId, name, purchaseUrl: purchaseUrl || null }),
    });
  },

  completeNeed(session, itemId) {
    return request(`/api/needs/${itemId}/complete`, {
      method: 'PATCH',
      body: JSON.stringify({ userId: session.userId, purchasedAt: today() }),
    });
  },

  getSubscriptions(session) {
    return request(`/api/users/${session.userId}/subscriptions`);
  },

  addSubscription(session, input) {
    return request(`/api/users/${session.userId}/subscriptions`, {
      method: 'POST',
      body: JSON.stringify({
        ...input,
        billingCycle: 'MONTHLY',
        managementUrl: input.managementUrl || null,
        shared: false,
      }),
    });
  },

  getReceipts(session) {
    return request(`/api/households/${session.householdId}/receipts?userId=${session.userId}`);
  },

  intakeReceipt(session, upload) {
    const form = new FormData();
    form.append('userId', session.userId);
    if (upload.file) {
      form.append('file', upload.file, upload.fileName);
    } else {
      form.append('file', { uri: upload.uri, name: upload.fileName, type: upload.mimeType } as unknown as Blob);
    }
    return requestForm(`/api/households/${session.householdId}/receipts/intake`, form);
  },

  confirmReceipt(session, receiptId, input) {
    return request(`/api/receipts/${receiptId}/confirm`, {
      method: 'PATCH',
      body: JSON.stringify({ userId: session.userId, ...input }),
    });
  },

  getAttention(session) {
    return request(`/api/attention?householdId=${session.householdId}&userId=${session.userId}`);
  },
};

type DemoStore = {
  members: HouseholdMember[];
  invitations: HouseholdInvitation[];
  fridge: FridgeItem[];
  householdItems: HouseholdItem[];
  needs: NeedListItem[];
  subscriptions: Subscription[];
  receipts: ReceiptView[];
};

const demoSession: Session = {
  userId: 'preview-user',
  email: 'preview@mindoff.local',
  userName: 'MindOFF Preview',
  householdId: 'preview-household',
  householdName: '우리 집 · Preview',
};

const storageKey = 'mindoff-preview-v2';
let memoryStore: DemoStore | null = null;

function initialDemoStore(): DemoStore {
  return {
    members: [
      {
        userId: demoSession.userId,
        email: demoSession.email,
        name: demoSession.userName,
        role: 'OWNER',
      },
    ],
    invitations: [],
    fridge: [
      {
        id: createId(),
        householdId: demoSession.householdId,
        name: '그릭요거트',
        purchasedAt: addDays(-3),
        expiresAt: addDays(1),
        status: 'ACTIVE',
      },
    ],
    householdItems: [
      {
        id: createId(),
        householdId: demoSession.householdId,
        name: '주방세제',
        purchasedAt: addDays(-24),
        finishedAt: null,
        predictedDays: null,
        repeatPurchase: true,
        purchaseUrl: 'https://www.example.com/',
        status: 'ACTIVE',
      },
    ],
    needs: [
      {
        id: createId(),
        householdId: demoSession.householdId,
        sourceType: 'MANUAL',
        sourceId: null,
        name: '생수',
        purchaseUrl: null,
        status: 'NEEDED',
      },
    ],
    subscriptions: [
      {
        id: createId(),
        userId: demoSession.userId,
        name: 'YouTube Premium',
        amount: 14900,
        billingCycle: 'MONTHLY',
        nextBillingAt: addDays(12),
        trialEndAt: null,
        managementUrl: 'https://www.youtube.com/paid_memberships',
        shared: false,
      },
    ],
    receipts: [
      {
        receipt: {
          id: createId(),
          householdId: demoSession.householdId,
          uploadedBy: demoSession.userId,
          merchantName: '동네마트',
          purchasedAt: addDays(-3),
          totalAmount: 23600,
          imageName: 'sample-receipt.jpg',
          imageContentType: 'image/jpeg',
          status: 'CONFIRMED',
          createdAt: new Date().toISOString(),
          confirmedAt: new Date().toISOString(),
        },
        lines: [],
      },
    ],
  };
}

function readDemoStore(): DemoStore {
  if (memoryStore) return memoryStore;
  try {
    const raw = globalThis.localStorage?.getItem(storageKey);
    if (!raw) {
      memoryStore = initialDemoStore();
    } else {
      const parsed = JSON.parse(raw) as Partial<DemoStore>;
      const initial = initialDemoStore();
      memoryStore = {
        ...initial,
        ...parsed,
        members: parsed.members ?? initial.members,
        invitations: parsed.invitations ?? initial.invitations,
      };
    }
  } catch {
    memoryStore = initialDemoStore();
  }
  return memoryStore;
}

function writeDemoStore(store: DemoStore) {
  memoryStore = store;
  try {
    globalThis.localStorage?.setItem(storageKey, JSON.stringify(store));
  } catch {
    // Native preview and privacy-restricted browsers can safely use memory only.
  }
}

const demoApi: MindoffApi = {
  async bootstrap() {
    await previewDelay();
    readDemoStore();
    return demoSession;
  },

  async getHome() {
    await previewDelay();
    const store = readDemoStore();
    const attentionLimit = addDays(2);
    const attentionCount = store.fridge.filter(
      (item) => item.status === 'ACTIVE' && item.expiresAt !== null && item.expiresAt <= attentionLimit,
    ).length;
    const needListCount = store.needs.filter((item) => item.status === 'NEEDED').length;
    const recordedFixedLivingCost = store.subscriptions.reduce((total, item) => {
      return total + (item.billingCycle === 'ANNUAL' ? Number(item.amount) / 12 : Number(item.amount));
    }, 0);
    const monthStart = `${today().slice(0, 7)}-01`;
    const receiptPurchaseTotal = store.receipts
      .filter((item) => item.receipt.status === 'CONFIRMED' && item.receipt.purchasedAt >= monthStart)
      .reduce((total, item) => total + Number(item.receipt.totalAmount), 0);
    return { attentionCount, needListCount, recordedFixedLivingCost, receiptPurchaseTotal };
  },

  async getHousehold() {
    await previewDelay();
    return {
      id: demoSession.householdId,
      name: demoSession.householdName,
      ownerId: demoSession.userId,
      members: [...readDemoStore().members],
    };
  },

  async getHouseholdInvitations() {
    await previewDelay();
    return [...readDemoStore().invitations];
  },

  async createHouseholdInvitation(_session, email) {
    const store = readDemoStore();
    const normalizedEmail = email.trim().toLowerCase();
    const existing = store.invitations.find(
      (candidate) => candidate.email === normalizedEmail && candidate.status === 'PENDING',
    );
    if (existing) return existing;
    const now = new Date();
    const expiresAt = new Date(now);
    expiresAt.setDate(expiresAt.getDate() + 7);
    const invitation: HouseholdInvitation = {
      id: createId(),
      householdId: demoSession.householdId,
      email: normalizedEmail,
      token: createId(),
      status: 'PENDING',
      expiresAt: expiresAt.toISOString(),
      createdAt: now.toISOString(),
      acceptedAt: null,
    };
    store.invitations.unshift(invitation);
    writeDemoStore(store);
    await previewDelay();
    return invitation;
  },

  async getFridge() {
    await previewDelay();
    return [...readDemoStore().fridge];
  },

  async addFridge(_session, name, expiresAt) {
    const store = readDemoStore();
    const item: FridgeItem = {
      id: createId(),
      householdId: demoSession.householdId,
      name,
      purchasedAt: today(),
      expiresAt,
      status: 'ACTIVE',
    };
    store.fridge.unshift(item);
    writeDemoStore(store);
    return item;
  },

  async finishFridge(_session, itemId) {
    const store = readDemoStore();
    const item = required(store.fridge.find((candidate) => candidate.id === itemId));
    item.status = 'FINISHED';
    store.needs.unshift({
      id: createId(),
      householdId: demoSession.householdId,
      sourceType: 'FRIDGE',
      sourceId: item.id,
      name: item.name,
      purchaseUrl: null,
      status: 'NEEDED',
    });
    writeDemoStore(store);
    return item;
  },

  async getHouseholdItems() {
    await previewDelay();
    return [...readDemoStore().householdItems];
  },

  async addHouseholdItem(_session, name, purchaseUrl) {
    const store = readDemoStore();
    const item: HouseholdItem = {
      id: createId(),
      householdId: demoSession.householdId,
      name,
      purchasedAt: today(),
      finishedAt: null,
      predictedDays: null,
      repeatPurchase: true,
      purchaseUrl: purchaseUrl || null,
      status: 'ACTIVE',
    };
    store.householdItems.unshift(item);
    writeDemoStore(store);
    return item;
  },

  async finishHouseholdItem(_session, itemId) {
    const store = readDemoStore();
    const item = required(store.householdItems.find((candidate) => candidate.id === itemId));
    item.status = 'FINISHED';
    item.finishedAt = today();
    item.predictedDays = Math.max(1, daysBetween(item.purchasedAt, item.finishedAt));
    store.needs.unshift({
      id: createId(),
      householdId: demoSession.householdId,
      sourceType: 'HOUSEHOLD_ITEM',
      sourceId: item.id,
      name: item.name,
      purchaseUrl: item.purchaseUrl,
      status: 'NEEDED',
    });
    writeDemoStore(store);
    return item;
  },

  async getNeeds() {
    await previewDelay();
    return [...readDemoStore().needs];
  },

  async addNeed(_session, name, purchaseUrl) {
    const store = readDemoStore();
    const item: NeedListItem = {
      id: createId(),
      householdId: demoSession.householdId,
      sourceType: 'MANUAL',
      sourceId: null,
      name,
      purchaseUrl: purchaseUrl || null,
      status: 'NEEDED',
    };
    store.needs.unshift(item);
    writeDemoStore(store);
    return item;
  },

  async completeNeed(_session, itemId) {
    const store = readDemoStore();
    const item = required(store.needs.find((candidate) => candidate.id === itemId));
    if (item.status !== 'NEEDED') return item;
    if (item.sourceType === 'HOUSEHOLD_ITEM' && item.sourceId) {
      const previous = required(store.householdItems.find((candidate) => candidate.id === item.sourceId));
      store.householdItems.unshift({
        id: createId(),
        householdId: demoSession.householdId,
        name: previous.name,
        purchasedAt: today(),
        finishedAt: null,
        predictedDays: previous.predictedDays,
        repeatPurchase: previous.repeatPurchase,
        purchaseUrl: previous.purchaseUrl,
        status: 'ACTIVE',
      });
    }
    item.status = 'PURCHASED';
    writeDemoStore(store);
    return item;
  },

  async getSubscriptions() {
    await previewDelay();
    return [...readDemoStore().subscriptions];
  },

  async addSubscription(_session, input) {
    const store = readDemoStore();
    const item: Subscription = {
      id: createId(),
      userId: demoSession.userId,
      name: input.name,
      amount: input.amount,
      billingCycle: 'MONTHLY',
      nextBillingAt: input.nextBillingAt,
      trialEndAt: input.trialEndAt,
      managementUrl: input.managementUrl || null,
      shared: false,
    };
    store.subscriptions.unshift(item);
    writeDemoStore(store);
    return item;
  },

  async getReceipts() {
    await previewDelay();
    return [...readDemoStore().receipts];
  },

  async intakeReceipt(_session, upload) {
    const store = readDemoStore();
    const receiptId = createId();
    const lines: ReceiptLine[] = [
      demoReceiptLine(receiptId, '우유', 2900, 'FRIDGE', addDays(7)),
      demoReceiptLine(receiptId, '주방세제', 6900, 'HOUSEHOLD_ITEM', null),
    ];
    const view: ReceiptView = {
      receipt: {
        id: receiptId,
        householdId: demoSession.householdId,
        uploadedBy: demoSession.userId,
        merchantName: '영수증 매장',
        purchasedAt: today(),
        totalAmount: 9800,
        imageName: upload.fileName,
        imageContentType: upload.mimeType,
        status: 'DRAFT',
        createdAt: new Date().toISOString(),
        confirmedAt: null,
      },
      lines,
    };
    store.receipts.unshift(view);
    writeDemoStore(store);
    await previewDelay();
    return view;
  },

  async confirmReceipt(_session, receiptId, input) {
    const store = readDemoStore();
    const view = required(store.receipts.find((candidate) => candidate.receipt.id === receiptId));
    view.receipt.merchantName = input.merchantName;
    view.receipt.purchasedAt = input.purchasedAt;
    view.receipt.totalAmount = input.totalAmount;
    view.receipt.status = 'CONFIRMED';
    view.receipt.confirmedAt = new Date().toISOString();
    view.lines = input.lines.map((line) => ({ ...line, id: createId(), receiptId }));
    for (const line of view.lines) {
      if (line.targetType === 'FRIDGE') {
        store.fridge.unshift({
          id: createId(), householdId: demoSession.householdId, name: line.name,
          purchasedAt: input.purchasedAt, expiresAt: line.expiresAt, status: 'ACTIVE',
        });
      } else if (line.targetType === 'HOUSEHOLD_ITEM') {
        store.householdItems.unshift({
          id: createId(), householdId: demoSession.householdId, name: line.name,
          purchasedAt: input.purchasedAt, finishedAt: null, predictedDays: null,
          repeatPurchase: true, purchaseUrl: null, status: 'ACTIVE',
        });
      }
    }
    writeDemoStore(store);
    await previewDelay();
    return view;
  },

  async getAttention() {
    await previewDelay();
    const store = readDemoStore();
    const limit = addDays(2);
    const items: AttentionItem[] = store.fridge
      .filter((item) => item.status === 'ACTIVE' && item.expiresAt !== null && item.expiresAt <= limit)
      .map((item) => ({
        type: 'FRIDGE_EXPIRY' as const,
        sourceId: item.id,
        dueAt: item.expiresAt!,
        title: item.name,
        message: item.expiresAt! < today() ? '기한 지남' : '곧 만료',
        daysFromToday: daysBetween(today(), item.expiresAt!),
      }));
    return items;
  },
};

export const api: MindoffApi = IS_DEMO_MODE ? demoApi : remoteApi;

export function today(): string {
  return new Date().toISOString().slice(0, 10);
}

function addDays(days: number): string {
  const value = new Date();
  value.setDate(value.getDate() + days);
  return value.toISOString().slice(0, 10);
}

function daysBetween(start: string, end: string): number {
  const milliseconds = new Date(`${end}T00:00:00Z`).getTime() - new Date(`${start}T00:00:00Z`).getTime();
  return Math.round(milliseconds / 86_400_000);
}

function createId(): string {
  return `preview-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function required<T>(value: T | undefined): T {
  if (value === undefined) throw new Error('미리보기 항목을 찾을 수 없습니다.');
  return value;
}

function demoReceiptLine(
  receiptId: string,
  name: string,
  price: number,
  targetType: ReceiptItemTarget,
  expiresAt: string | null,
): ReceiptLine {
  return {
    id: createId(), receiptId, name, quantity: 1, unitPrice: price,
    lineTotal: price, targetType, expiresAt,
  };
}

function previewDelay() {
  return new Promise<void>((resolve) => setTimeout(resolve, 90));
}
