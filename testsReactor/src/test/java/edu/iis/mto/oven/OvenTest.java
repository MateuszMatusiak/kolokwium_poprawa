package edu.iis.mto.oven;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OvenTest {

	@Mock
	private HeatingModule heatingModule;
	@Mock
	private Fan fan;
	private Oven oven;
	private BakingProgram bakingProgram;

	List<ProgramStage> stages;
	int initialTemp;

	@BeforeEach
	void setUp() {
		oven = new Oven(heatingModule, fan);
		stages = new ArrayList<>();
		stages.add(ProgramStage.builder().withTargetTemp(150).withStageTime(30).withHeat(HeatType.THERMO_CIRCULATION).build());
		stages.add(ProgramStage.builder().withTargetTemp(200).withStageTime(45).withHeat(HeatType.THERMO_CIRCULATION).build());
		stages.add(ProgramStage.builder().withTargetTemp(170).withStageTime(20).withHeat(HeatType.THERMO_CIRCULATION).build());
		initialTemp = 100;
		bakingProgram = BakingProgram.builder().withStages(stages).withInitialTemp(initialTemp).withCoolAtFinish(true).build();
	}

	@Test
	void checkEverythingOK() {
		BakingResult result = oven.runProgram(bakingProgram);
		assertTrue(result.isSuccess());
	}

	@Test
	void ovenShouldInvokeFan() {
		oven.runProgram(bakingProgram);
		verify(fan, times(4)).on();
		verify(fan, times(3)).off();
	}

	@Test
	void fanShouldBeOff() {
		{
			stages.clear();
			stages.add(ProgramStage.builder().withTargetTemp(180).withHeat(HeatType.GRILL).withStageTime(60).build());
			stages.add(ProgramStage.builder().withTargetTemp(100).withHeat(HeatType.HEATER).withStageTime(60).build());
			stages.add(ProgramStage.builder().withTargetTemp(100).withHeat(HeatType.HEATER).withStageTime(40).build());
			bakingProgram = BakingProgram.builder().withStages(stages).withInitialTemp(initialTemp).withCoolAtFinish(false).build();

			oven.runProgram(bakingProgram);

			verify(fan, times(0)).on();
			assertFalse(fan.isOn());
		}
	}

	@Test
	void ovenHasProblemWithHeating() throws HeatingException {
		doThrow(HeatingException.class).when(heatingModule).heater(any());
		BakingResult result = oven.runProgram(bakingProgram);

		assertFalse(result.isSuccess());
	}

	@Test
	void ovenShouldInvokeHeatingProgram() throws HeatingException {
		{
			stages.clear();
			stages.add(ProgramStage.builder().withTargetTemp(180).withHeat(HeatType.GRILL).withStageTime(60).build());
			stages.add(ProgramStage.builder().withTargetTemp(100).withHeat(HeatType.HEATER).withStageTime(60).build());
			stages.add(ProgramStage.builder().withTargetTemp(100).withHeat(HeatType.HEATER).withStageTime(40).build());
			bakingProgram = BakingProgram.builder().withStages(stages).withInitialTemp(initialTemp).withCoolAtFinish(true).build();

			oven.runProgram(bakingProgram);
			verify(heatingModule, times(1)).grill(any());
			verify(heatingModule, times(3)).heater(any());
		}
	}
}
