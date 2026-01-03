import { useState, JSX } from "react"
import { Search, BadgeCheck, ChevronDown } from "lucide-react"
import { useKeycloak } from "@react-keycloak/web"
import ReactPaginate from "react-paginate"

import { UserRank, Player } from "../../types/app.types";
import { PlayerSearchParams, PageableResponse } from "../../types/api.types";
import { USER_RARITY_COLOR } from "../../config/app.config";
import { InputComponents, DisplayComponents, FeatureComponents } from "../../components/app";
import { useSearchPlayers } from "../../hooks/player/usePlayer";
import { useSendFriendRequest, useAllFriendships, useModifyFriendRequest } from "../../hooks/friend/useFriends";

const { Card, CardContent, CardHeader } = DisplayComponents
const { Input } = InputComponents
const { PlayerRows } = FeatureComponents

export const Players = () => {
  const { keycloak } = useKeycloak();

  const [searchParams, setSearchParams] = useState<PlayerSearchParams>({
    username: "", rank: "" as UserRank, page: 0, size: 20
  });
  const { data: playersResponse = {} as PageableResponse<Player>, isLoading, isError } =
      useSearchPlayers(searchParams);
  const { data: friendshipRequestsData } = useAllFriendships();
  const sendFriendshipRequest = useSendFriendRequest();
  const updateFriendshipRequest = useModifyFriendRequest();

  const userId = keycloak.tokenParsed?.sub as string;
  const { content: players = [] } = playersResponse;
  // Ensure friendshipRequests is always an array
  const friendshipRequests = Array.isArray(friendshipRequestsData) ? friendshipRequestsData : [];

  const totalPages = playersResponse.totalPages ?? 0;
  const currentPage = playersResponse.number ?? searchParams.page;

  return (
      <div className="space-y-8">
      <div className="space-y-6">
          <div>
              <h1 className="mb-2">Players</h1>
              <p className="text-muted-foreground">Find top players and make new friends</p>
          </div>
       </div>
        <Card className="w-full max-w-6xl border-border bg-card">
          <CardHeader className="gap-2">
            <div className="grid w-full grid-cols-1 gap-3 sm:grid-cols-2">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                    value={searchParams.username}
                    onChange={(e) =>
                        setSearchParams((prev) => ({ ...prev, username: e.target.value, page: 0}))
                    }
                    placeholder="Search by username"
                    className="h-11 pl-9"
                />
              </div>

              <div className="input h-11 w-full flex items-center justify-center gap-2 px-3">
                <BadgeCheck className="h-4 w-4 shrink-0 text-muted-foreground block" />

                <select
                    value={searchParams.rank ?? ""}
                    onChange={(e) =>
                        setSearchParams((prev) => ({...prev, rank: e.target.value as UserRank, page: 0 }))
                    }
                    className="h-full flex-1 min-w-0 -mt-1 appearance-none bg-transparent text-foreground outline-none"
                    aria-label="Filter by rank"
                >
                  <option value="">All ranks</option>
                  {Object.keys(USER_RARITY_COLOR).map((r) => (
                      <option key={r} value={r}>
                        {r}
                      </option>
                  ))}
                </select>

                <ChevronDown className="h-4 w-4 shrink-0 text-muted-foreground" />
              </div>
            </div>
          </CardHeader>

          <CardContent>
            <div className="overflow-hidden rounded-lg border border-border">
              <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                      <thead className="bg-muted/40">
                      <tr className="border-b border-border">
                          <th scope="col"
                              className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                              Player
                          </th>
                          <th scope="col"
                              className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                              Rank
                          </th>
                          <th scope="col"
                              className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-muted-foreground">
                              EXP
                          </th>
                          <th scope="col"
                              className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wide text-muted-foreground">
                              Friendship
                          </th>
                      </tr>
                      </thead>

                      <tbody className="bg-card">
                      {isLoading && <>{LoadingBlueprint}</>}

                      {!isLoading && isError && (
                          <tr className="border-t border-border">
                              <td colSpan={4} className="px-4 py-10 text-center text-sm text-destructive">
                                  Something went wrong while searching players.
                              </td>
                          </tr>
                      )}

                      {!isLoading && !isError && players.length === 0 && (
                          <tr className="border-t border-border">
                              <td colSpan={4} className="px-4 py-10 text-center text-sm text-muted-foreground">
                                  No players found. Try adjusting your filters.
                              </td>
                          </tr>
                      )}
                      {
                          !isLoading &&
                          !isError &&
                          PlayerRows({
                              userId,
                              players,
                              friendshipRequests,
                              sendFriendshipRequest,
                              updateFriendshipRequest
                          })}
                      </tbody>
                  </table>
              </div>

                {!isLoading && !isError && totalPages > 1 && (
                    <div className="border-t border-border px-4 py-3">
                        <ReactPaginate
                            forcePage={currentPage}
                            pageCount={totalPages}
                            onPageChange={(e) => setSearchParams((prev) => ({...prev, page: e.selected}))}
                            previousLabel="Prev"
                            nextLabel="Next"
                            breakLabel="…"
                            marginPagesDisplayed={1}
                            pageRangeDisplayed={3}
                            disabledClassName="opacity-50 pointer-events-none"
                            containerClassName="flex items-center justify-center gap-2"
                        pageLinkClassName="h-9 min-w-[2.25rem] px-3 border border-border rounded-md flex items-center justify-center text-sm"
                        activeLinkClassName="bg-secondary font-semibold"
                        previousLinkClassName="h-9 px-3 border border-border rounded-md flex items-center justify-center text-sm"
                        nextLinkClassName="h-9 px-3 border border-border rounded-md flex items-center justify-center text-sm"
                        breakLinkClassName="h-9 px-3 border border-border rounded-md flex items-center justify-center text-sm"
                    />
                  </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
  );
};

const LoadingBlueprint = (): JSX.Element[] => {
  return Array.from({ length: 6 }).map((_, i) => (
      <tr key={i} className="border-t border-border">
        <td className="px-4 py-4">
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 animate-pulse rounded-full bg-muted" />
            <div className="h-4 w-40 animate-pulse rounded bg-muted" />
          </div>
        </td>
        <td className="px-4 py-4">
          <div className="h-6 w-24 animate-pulse rounded bg-muted" />
        </td>
        <td className="px-4 py-4">
          <div className="h-4 w-20 animate-pulse rounded bg-muted" />
        </td>
        <td className="px-4 py-4">
          <div className="ml-auto h-9 w-32 animate-pulse rounded bg-muted" />
        </td>
      </tr>
  ))
}
