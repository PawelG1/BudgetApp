import { useCallback, useEffect, useState } from "react";
import { GroupDebt, Id, groupsApi } from "../../api/groupsApi";
import { useParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import styles from "./Group.module.scss";

const GroupDebtsPage = () => {
  const { groupId } = useParams();
  const { user } = useAuth();
  const [debts, setDebts] = useState<GroupDebt[]>([]);
  const [ownerId, setOwnerId] = useState<Id | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const currentUserId = user?.id !== undefined ? String(user.id) : "";

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
      console.error("Bˆ¥d pobierania dˆug¢w:", error);
      setDebts([]);
      setErrorMessage(getErrorMessage(error, "Nie udaˆo si© pobra† dˆug¢w grupy."));
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
        console.error("Bˆ¥d pobierania dˆug¢w:", error);
        setDebts([]);
        setErrorMessage(getErrorMessage(error, "Nie udaˆo si© pobra† dˆug¢w grupy."));
      });

    return () => {
      ignore = true;
    };
  }, [fetchDebtsData]);

  const handleDeleteDebt = async (debtId: Id) => {
    if (!window.confirm("Czy na pewno chcesz usunąć ten dług?")) return;

    try {
      setErrorMessage("");
      await groupsApi.deleteDebt(debtId);
      refreshDebts();
    } catch (error: unknown) {
      console.error("Błąd usuwania długu:", error);
      setErrorMessage(getErrorMessage(error, "Nie udało się usunąć długu."));
    }
  };

  const canManageDebt = (debt: GroupDebt) =>
    String(ownerId) === currentUserId ||
    String(debt.debtor.id) === currentUserId ||
    String(debt.creditor.id) === currentUserId;

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
            {canManageDebt(debt) && (
              <button
                type="button"
                className={styles.deleteButton}
                onClick={() => handleDeleteDebt(debt.id)}
              >
                Usuń
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default GroupDebtsPage;
