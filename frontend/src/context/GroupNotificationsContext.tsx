import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";

interface GroupNotificationsContextType {
  /** Inkrementuje się przy każdym powiadomieniu WS – komponenty reagują na zmianę */
  dataVersion: number;
  /** Wywołaj gdy przyjdzie powiadomienie WS */
  bumpDataVersion: () => void;
}

const GroupNotificationsContext = createContext<GroupNotificationsContextType>({
  dataVersion: 0,
  bumpDataVersion: () => {},
});

export const GroupNotificationsProvider = ({ children }: { children: ReactNode }) => {
  const [dataVersion, setDataVersion] = useState(0);

  const bumpDataVersion = useCallback(() => {
    setDataVersion((v) => v + 1);
  }, []);

  const value = useMemo(
    () => ({ dataVersion, bumpDataVersion }),
    [dataVersion, bumpDataVersion]
  );

  return (
    <GroupNotificationsContext.Provider value={value}>
      {children}
    </GroupNotificationsContext.Provider>
  );
};

export const useGroupNotificationsCtx = () =>
  useContext(GroupNotificationsContext);
