package com.gmail.goosius.siegewar.utils;

import com.gmail.goosius.siegewar.enums.SiegeSide;
import com.gmail.goosius.siegewar.enums.SiegeWarPermissionNodes;
import com.gmail.goosius.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import org.bukkit.entity.Player;

import java.util.Map;

public class SiegeWarAllegianceUtil {

    public static SiegeSide calculateCandidateSiegePlayerSide(Player deadPlayer, Town deadResidentTown, Siege candidateSiege) {

        Resident resident = TownyUniverse.getInstance().getResident(deadPlayer.getName());
        Map<String,Boolean> permissions = TownyPerms.getResidentPerms(resident);
        boolean townNode = permissions.getOrDefault(SiegeWarPermissionNodes.SIEGEWAR_TOWN_SIEGE_BATTLE_POINTS.getNode(),false);
        boolean nationNode = permissions.getOrDefault(SiegeWarPermissionNodes.SIEGEWAR_NATION_SIEGE_BATTLE_POINTS.getNode(),false);
        boolean nationNode2 = permissions.getOrDefault("siegewar.nation.siege.*",false);
        boolean townNode2 = permissions.getOrDefault("siegewar.town.siege.*",false);

        boolean townGuard = townNode || townNode2;
        boolean alliedSoldier = nationNode || nationNode2;
        //Look for defender
        Government defendingGovernment = candidateSiege.getDefender();
        switch (candidateSiege.getSiegeType()) {
            case CONQUEST:
            case SUPPRESSION:
            case LIBERATION:
                //In the above sieges, defenders can be town guards
                if (isTownGuard(deadPlayer, deadResidentTown, defendingGovernment,townGuard))
                    return SiegeSide.DEFENDERS;
            case REVOLT:
                //In the above sieges, defenders can be nation/allied soldiers
                if (isNationSoldierOrAlliedSoldier(deadPlayer, deadResidentTown, defendingGovernment,alliedSoldier))
                    return SiegeSide.DEFENDERS;
        }

        //Look for attacker
        Government attackingGovernment = candidateSiege.getAttacker();
        switch (candidateSiege.getSiegeType()) {
            case REVOLT:
                //In the above sieges, attackers can be town guards
                if (isTownGuard(deadPlayer, deadResidentTown, attackingGovernment,townGuard))
                    return SiegeSide.ATTACKERS;
            case CONQUEST:
            case SUPPRESSION:
            case LIBERATION:
                //In the above sieges, attackers can be nation/allied soldiers
                if (isNationSoldierOrAlliedSoldier(deadPlayer, deadResidentTown, attackingGovernment,alliedSoldier))
                    return SiegeSide.ATTACKERS;
        }
        return SiegeSide.NOBODY;
    }

    private static boolean isTownGuard(Player player, Town residentTown, Government governmentToCheck, boolean townPerms) {
        return residentTown == governmentToCheck
                && townPerms;
    }

    private static boolean isNationSoldierOrAlliedSoldier(Player player, Town residentTown, Government governmentToCheck, boolean nationPerms) {
        if(!residentTown.hasNation())
            return false;

        Nation nation = TownyAPI.getInstance().getTownNationOrNull(residentTown);

        if(!nationPerms)
            return false;

        if(governmentToCheck instanceof Nation) {
            //The government-to-check is a nation
            return nation == governmentToCheck
                    || nation.hasMutualAlly((Nation) governmentToCheck);
        } else if (((Town)governmentToCheck).hasNation()) {
            //The government-to-check is a nation town
            return nation == TownyAPI.getInstance().getTownNationOrNull((Town) governmentToCheck)
                    || nation.hasMutualAlly(TownyAPI.getInstance().getTownNationOrNull((Town) governmentToCheck));
        } else {
            //The government-to-check is a non-nation town. Nation soldiers cannot contribute
            return false;
        }
    }

    public static boolean isSideHostileToTown(SiegeSide gunnerSiegeSide, Siege siege) {
        switch (gunnerSiegeSide) {
            case ATTACKERS:
                return siege.isConquestSiege() || siege.isSuppressionSiege();
            case DEFENDERS:
                return siege.isLiberationSiege() || siege.isRevoltSiege();
            default:
                return false;
        }
    }

    /**
     * Determine if a player is on the town-friendly side of a given siege
     */
    public static boolean isPlayerOnTownFriendlySide(Player player, Resident resident, Siege siege) {
        Town gunnerResidentTown = resident.getTownOrNull();
        SiegeSide playerSiegeSide = calculateCandidateSiegePlayerSide(player, gunnerResidentTown, siege);
        switch(playerSiegeSide) {
            case DEFENDERS:
                return siege.isConquestSiege() || siege.isSuppressionSiege();
            case ATTACKERS:
                return siege.isLiberationSiege() || siege.isRevoltSiege();
            default:
                return false;
        }
    }

    /**
     * Determine if a player is on the town-hostile side of a given siege
     */
    public static boolean isPlayerOnTownHostileSide(Player player, Resident resident, Siege siege) {
        Town gunnerResidentTown = resident.getTownOrNull();
        SiegeSide playerSiegeSide = calculateCandidateSiegePlayerSide(player, gunnerResidentTown, siege);
        switch(playerSiegeSide) {
            case ATTACKERS:
                return siege.isConquestSiege() || siege.isSuppressionSiege(); 
            case DEFENDERS:
                return siege.isLiberationSiege() || siege.isRevoltSiege();
            default:
                return false;
        }
    }
}
