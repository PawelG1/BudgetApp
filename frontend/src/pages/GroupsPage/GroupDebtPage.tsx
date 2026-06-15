import { useCallback, useEffect, useRef, useState } from "react";
import { GroupDebt, Id, groupsApi } from "../../api/groupsApi";
import { useParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useBalance } from "../../components/BalanceBar/useBalance";
import { useGroupNotificationsCtx } from "../../context/GroupNotificationsContext";
import styles from "./Group.module.scss";
import ConfirmModal from "../../components/ConfirmModal/ConfirmModal";

const GroupDebtsPage = () => {
  const { groupId } = useParams();
  const { user } = useAuth();
  const { refreshBalance } = useBalance();
  const { dataVersion } = useGroupNotificationsCtx();
  const [debts, setDebts] = useState<GroupDebt[]>([]);
  const [ownerId, setOwnerId] = useState<Id | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [debtToDelete, setDebtToDelete] = useState<GroupDebt | null>(null);
  const currentUserId = user?.id !== undefined ? String(user.id) : "";
  // Ref zapobiega wywołaniu przy pierwszym renderze (dataVersion === 0)
  const isFirstRender = useRef(true);

  const getErrorMessage = (error: unknown, fallback: string) => {
    if (error instanceof Error && error.message.trim()) {
      return error.message.replace(/^Wystąpił błąd:\s*/i, "");
    }

    return fallback;
  };

  const fetchDebtsData = useCallback(async () => {
    if (!groupId) return null;

    const [debtsData, groupsData] = await Promise.all([
      groupsApi.getDebts(groupId),
      groupsApi.getGroups(),
    ]);

    return {
      debtsData,
      ownerId:
        groupsData.find((group) => String(group.id) === String(groupId))
          ?.ownerId ?? null,
    };
  }, [groupId]);

  const refreshDebts = useCallback(async () => {
    try {
      const data = await fetchDebtsData();
      if (!data) return;

      setErrorMessage("");
      setDebts(data.debtsData);
      setOwnerId(data.ownerId);
    } catch (error: unknown) {
      console.error("Błąd pobierania długów:", error);
      setDebts([]);
      setErrorMessage(getErrorMessage(error, "Nie udało się pobrać długów grupy."));
    }
  }, [fetchDebtsData]);

  useEffect(() => {
    let ignore = false;

    fetchDebtsData()
      .then((data) => {
        if (ignore || !data) return;
        setErrorMessage("");
        setDebts(data.debtsData);
        setOwnerId(data.ownerId);
      })
      .catch((error: unknown) => {
        if (ignore) return;
        console.error("Błąd pobierania długów:", error);
        setDebts([]);
        setErrorMessage(getErrorMessage(error, "Nie udało się pobrać długów grupy."));
      });

    return () => {
      ignore = true;
    };
  }, [fetchDebtsData]);

  // Reaguje na powiadomienia WebSocket przez React Context
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    refreshDebts();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dataVersion]);

  // Polling co 4 sekundy — rekurencyjny setTimeout, niezawodny wzorzec React
  useEffect(() => {
    if (!groupId) return;

    let cancelled = false;
    let timerId: ReturnType<typeof setTimeout>;

    const poll = async () => {
      if (cancelled) return;
      try {
        const [debtsData, groupsData] = await Promise.all([
          groupsApi.getDebts(groupId),
          groupsApi.getGroups(),
        ]);
        if (!cancelled) {
          setDebts(debtsData);
          setOwnerId(
            groupsData.find((g) => String(g.id) === String(groupId))?.ownerId ?? null
          );
        }
      } catch {
        // ignorujemy błędy pollingu — nie nadpisujemy wyświetlanych danych
      }
      if (!cancelled) {
        timerId = setTimeout(poll, 4000);
      }
    };

    timerId = setTimeout(poll, 4000);

    return () => {
      cancelled = true;
      clearTimeout(timerId);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [groupId]); // groupId z URL, stały przez cały czas życia komponentu

  const handleDeleteDebt = async () => {
    if (!debtToDelete) return;

    try {
      setErrorMessage("");
      await groupsApi.deleteDebt(debtToDelete.id);
      setDebts((prev) => prev.filter((d) => String(d.id) !== String(debtToDelete.id)));
      setDebtToDelete(null);
    } catch (error: unknown) {
      console.error("Błąd usuwania długu:", error);
      setErrorMessage(getErrorMessage(error, "Nie udało się usunąć długu."));
    }
  };

  const canManageDebt = (debt: GroupDebt) =>
    String(ownerId) === currentUserId ||
    String(debt.debtor.id) === currentUserId ||
    String(debt.creditor.id) === currentUserId;

  const canMarkDebtAsPaid = (debt: GroupDebt) =>
    String(debt.debtor.id) === currentUserId && !debt.paidByDebtor;

  const canConfirmDebtPayment = (debt: GroupDebt) =>
    String(debt.creditor.id) === currentUserId &&
    debt.paidByDebtor &&
    !debt.confirmedByCreditor;

  const getDebtStatusLabel = (debt: GroupDebt) => {
    if (debt.confirmedByCreditor) return "Spłata potwierdzona";
    if (debt.paidByDebtor) return "Oczekuje na potwierdzenie";
    return "Nieopłacony";
  };

  const handleMarkDebtAsPaid = async (debtId: Id) => {
    try {
      setErrorMessage("");
      const updatedDebt = await groupsApi.markDebtAsPaid(debtId);
      setDebts((prev) =>
        prev.map((d) => (String(d.id) === String(debtId) ? updatedDebt : d))
      );
      refreshBalance(null);
    } catch (error: unknown) {
      console.error("Błąd oznaczania długu jako opłaconego:", error);
      setErrorMessage(
        getErrorMessage(error, "Nie udało się oznaczyć długu jako opłaconego.")
      );
    }
  };

  const handleConfirmDebtPayment = async (debtId: Id) => {
    try {
      setErrorMessage("");
      const updatedDebt = await groupsApi.confirmDebtPayment(debtId);
      setDebts((prev) =>
        prev.map((d) => (String(d.id) === String(debtId) ? updatedDebt : d))
      );
      refreshBalance(null);
    } catch (error: unknown) {
      console.error("Błąd potwierdzania spłaty długu:", error);
      setErrorMessage(
        getErrorMessage(error, "Nie udało się potwierdzić spłaty długu.")
      );
    }
  };

  return (
    <div className={styles.container}>
      <h2>Długi w grupie</h2>

      {errorMessage && <p className={styles.errorMessage}>{errorMessage}</p>}

      <ul className={styles.debtsList}>
        {debts.map((debt) => (
          <li key={debt.id}>
            <strong className={styles.debtorName}>{debt.debtor.email}</strong>{" "}
            jest winien{" "}
            <strong className={styles.creditorName}>
              {debt.creditor.email}
            </strong>{" "}
            {debt.amount.toFixed(2)} zł za <strong>{debt.title}</strong>
            <span
              className={`${styles.statusBadge} ${
                debt.confirmedByCreditor
                  ? styles.statusPaid
                  : debt.paidByDebtor
                    ? styles.statusPending
                    : styles.statusOpen
              }`}
            >
              {getDebtStatusLabel(debt)}
            </span>
            {canMarkDebtAsPaid(debt) && (
              <button
                type="button"
                className={styles.button}
                onClick={() => handleMarkDebtAsPaid(debt.id)}
              >
                Oznacz jako opłacony
              </button>
            )}
            {canConfirmDebtPayment(debt) && (
              <button
                type="button"
                className={styles.button}
                onClick={() => handleConfirmDebtPayment(debt.id)}
              >
                Potwierdź spłatę
              </button>
            )}
            {canManageDebt(debt) && (
              <button
                type="button"
                className={styles.deleteButton}
                onClick={() => setDebtToDelete(debt)}
              >
                Usuń
              </button>
            )}
          </li>
        ))}
      </ul>

      <ConfirmModal
        visible={Boolean(debtToDelete)}
        title="Usuń dług"
        message="Czy na pewno chcesz usunąć ten dług?"
        confirmLabel="Usuń"
        onConfirm={handleDeleteDebt}
        onCancel={() => setDebtToDelete(null)}
      />
    </div>
  );
};

export default GroupDebtsPage;
