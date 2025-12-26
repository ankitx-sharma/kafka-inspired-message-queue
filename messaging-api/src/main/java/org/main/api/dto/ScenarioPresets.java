package org.main.api.dto;

public final class ScenarioPresets {
	private ScenarioPresets() {}
	
	public static ScenarioPresetDto from(Scenario scenario) {
		return switch(scenario) {
		case A -> new ScenarioPresetDto(scenario, 4, 50, 50, 30);     // small burst, no spill
		case B -> new ScenarioPresetDto(scenario, 2, 5, 50, 200);     // spike -> spill to disk
		case C -> new ScenarioPresetDto(scenario, 2, 10, 3_000, 60);  // slow workers -> backlog
		case D -> new ScenarioPresetDto(scenario, 1, 3, 2_000, 120);  // saturate hard (spill) then recovery step
		};
	}
}