export type Session = {
  userId: string;
  email: string;
  userName: string;
  householdId: string;
  householdName: string;
};

export type HomeSummary = {
  attentionCount: number;
  needListCount: number;
  recordedFixedLivingCost: number;
  receiptPurchaseTotal: number;
};

export type HouseholdMember = {
  userId: string;
  email: string;
  name: string;
  role: 'OWNER' | 'MEMBER';
};

export type HouseholdDetails = {
  id: string;
  name: string;
  ownerId: string;
  members: HouseholdMember[];
};

export type HouseholdInvitation = {
  id: string;
  householdId: string;
  email: string;
  token: string;
  status: 'PENDING' | 'ACCEPTED' | 'EXPIRED';
  expiresAt: string;
  createdAt: string;
  acceptedAt: string | null;
};

export type FridgeItem = {
  id: string;
  householdId: string;
  name: string;
  purchasedAt: string;
  expiresAt: string | null;
  status: 'ACTIVE' | 'FINISHED';
};

export type HouseholdItem = {
  id: string;
  householdId: string;
  name: string;
  purchasedAt: string;
  finishedAt: string | null;
  predictedDays: number | null;
  repeatPurchase: boolean;
  purchaseUrl: string | null;
  status: 'ACTIVE' | 'FINISHED';
};

export type NeedListItem = {
  id: string;
  householdId: string;
  sourceType: 'FRIDGE' | 'HOUSEHOLD_ITEM' | 'MANUAL';
  sourceId: string | null;
  name: string;
  purchaseUrl: string | null;
  status: 'NEEDED' | 'PURCHASED';
};

export type Subscription = {
  id: string;
  userId: string;
  name: string;
  amount: number;
  billingCycle: 'MONTHLY' | 'ANNUAL' | 'CUSTOM';
  nextBillingAt: string | null;
  trialEndAt: string | null;
  managementUrl: string | null;
  shared: boolean;
};

export type ReceiptStatus = 'DRAFT' | 'CONFIRMED';
export type ReceiptItemTarget = 'FRIDGE' | 'HOUSEHOLD_ITEM' | 'IGNORE';

export type Receipt = {
  id: string;
  householdId: string;
  uploadedBy: string;
  merchantName: string;
  purchasedAt: string;
  totalAmount: number;
  imageName: string | null;
  imageContentType: string | null;
  status: ReceiptStatus;
  createdAt: string;
  confirmedAt: string | null;
};

export type ReceiptLine = {
  id: string;
  receiptId: string;
  name: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  targetType: ReceiptItemTarget;
  expiresAt: string | null;
};

export type ReceiptView = {
  receipt: Receipt;
  lines: ReceiptLine[];
};

export type AttentionItem = {
  type: 'FRIDGE_EXPIRY' | 'USAGE_PREDICTION' | 'TRIAL_END';
  sourceId: string;
  dueAt: string;
  title: string;
  message: string;
  daysFromToday: number;
};
