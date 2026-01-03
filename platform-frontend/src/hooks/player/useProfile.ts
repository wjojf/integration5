import {useMutation, useQuery, useQueryClient} from "@tanstack/react-query";

import type { UpdatePlayerRequest } from "../../types/api.types";

import { QUERY_KEYS } from "../../config/api.config";
import { playerService } from "../../service/player.service";

const { PLAYER } = QUERY_KEYS

export const useProfile = () => {
    return useQuery({
        queryKey: PLAYER.PROFILE,
        queryFn: () => playerService.getProfile(),
        staleTime: 5 * 60 * 1000,
    });
};

export const useUpdateProfile = () => {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: UpdatePlayerRequest) => playerService.updateProfile(data),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: PLAYER.PROFILE }),
    });
};
