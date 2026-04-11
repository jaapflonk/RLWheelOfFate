package com.wheeloffate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BossPresets
{
	public static final List<String> BOSSES;

	static
	{
		List<String> list = Arrays.asList(
			"Abyssal Sire",
			"Alchemical Hydra",
			"Barbarian Assault",
			"Callisto",
			"Cerberus",
			"Chambers of Xeric",
			"Chaos Elemental",
			"Colosseum",
			"Commander Zilyana",
			"Corporeal Beast",
			"Corrupted Gauntlet",
			"Dagannoth Kings",
			"Duke Sucellus",
			"Fight Caves",
			"Gauntlet",
			"General Graardor",
			"Giant Mole",
			"Grotesque Guardians",
			"Inferno",
			"K'ril Tsutsaroth",
			"Kalphite Queen",
			"King Black Dragon",
			"Kraken",
			"Kree'arra",
			"Nex",
			"Nightmare",
			"Pest Control",
			"Phantom Muspah",
			"Phosani's Nightmare",
			"Sarachnis",
			"Scorpia",
			"Tempoross",
			"The Leviathan",
			"The Whisperer",
			"Theatre of Blood",
			"Thermonuclear Smoke Devil",
			"Tombs of Amascut",
			"Vardorvis",
			"Venenatis",
			"Vet'ion",
			"Vorkath",
			"Wintertodt",
			"Zalcano",
			"Zulrah"
		);
		BOSSES = Collections.unmodifiableList(list);
	}

	private BossPresets()
	{
	}
}
