import { PlusCircle } from "lucide-react";

import { Card, CardContent } from "@/components/common";
import {
  formatGamificationDate,
  gamificationActionLabel,
} from "@/components/gamification/gamificationFormatting";
import type { XpTransaction } from "@/types/gamification";

export function XpHistoryList({
  transactions,
}: {
  transactions: XpTransaction[];
}) {
  return (
    <div className="grid gap-3">
      {transactions.map((transaction) => (
        <Card key={transaction.id}>
          <CardContent className="flex items-center gap-3 p-4">
            <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-600">
              <PlusCircle className="size-5" />
            </span>
            <div className="min-w-0 flex-1">
              <p className="font-semibold text-slate-900">
                {gamificationActionLabel(transaction.actionType)}
              </p>
              <p className="mt-1 truncate text-xs text-slate-500">
                {transaction.description || "CampusOne contribution"}
              </p>
            </div>
            <div className="text-right">
              <p className="font-bold text-emerald-700">
                +{transaction.points} XP
              </p>
              <p className="mt-1 text-[11px] text-slate-400">
                {formatGamificationDate(transaction.createdAt)}
              </p>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

