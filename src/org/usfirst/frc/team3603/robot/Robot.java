package org.usfirst.frc.team3603.robot;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.CounterBase.EncodingType;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.I2C.Port;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.PIDOutput;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends IterativeRobot {
	
	private static final String Tim = "Tim"; //String for Tim's profile
	private static final String Troy = "Troy"; //String for Troy's profile
	private static final String Spencer = "Spencer"; //String for Spencer's profile
	private static final String Collin = "Collin"; //String for Collin's profile
	private static final String Connor = "Connor";
	
	private String driverString; //String for chosen driver
	private SendableChooser<String> driver = new SendableChooser<>(); //Driver chooser
	private String manString; //String for manipulator
	private SendableChooser<String> man = new SendableChooser<>(); //Manipulator chooser
	
	final static DoubleSolenoid.Value out = DoubleSolenoid.Value.kForward; //Piston out value
	static final DoubleSolenoid.Value in = DoubleSolenoid.Value.kReverse; //Piston in value
	
	//All of these are individual speed controllers
	WPI_TalonSRX leftFront = new WPI_TalonSRX(10);
	WPI_TalonSRX leftMiddle = new WPI_TalonSRX(11);
	WPI_TalonSRX leftBack = new WPI_TalonSRX(12);
	WPI_TalonSRX rightFront = new WPI_TalonSRX(4);
	WPI_TalonSRX rightMiddle = new WPI_TalonSRX(5);
	WPI_TalonSRX rightBack = new WPI_TalonSRX(6);
	//This groups the speed controllers into left and right
	SpeedControllerGroup left = new SpeedControllerGroup(leftFront, leftMiddle, leftBack);
	SpeedControllerGroup right = new SpeedControllerGroup(rightFront, rightMiddle, rightBack);
	//This groups them into the new type of RobotDrive
	DifferentialDrive mainDrive = new DifferentialDrive(left, right);
	
	WPI_TalonSRX leftHolder = new WPI_TalonSRX(9);//Leftholder speedcontroller
	WPI_TalonSRX rightHolder = new WPI_TalonSRX(8);//Rightholder speedcontroller
	WPI_TalonSRX cubeLift = new WPI_TalonSRX(3); //Cube lift speed controller
	WPI_TalonSRX arm = new WPI_TalonSRX(7);
	Servo release = new Servo(0);
	
	Compressor compressor = new Compressor();
	DoubleSolenoid omni = new DoubleSolenoid(0, 1); //Omni solenoid
	DoubleSolenoid shift = new DoubleSolenoid(2, 3);//Transmission solenoid
	
	Joystick joy1 = new Joystick(0); //Large twist-axis joystick
	Joystick joy2 = new Joystick(1); //Xbox controller
	MyEncoder liftEnc = new MyEncoder(cubeLift, false, 1.0); //Encoder for the cube lift
	double mult = (4*Math.PI)/60; //Multiplier for driveEnc TODO multiplier
	TouchlessEncoder driveEnc = new TouchlessEncoder(2, mult);
	Encoder armEnc = new Encoder(0, 1, false, EncodingType.k2X);
	WPI_TalonSRX pidStore = new WPI_TalonSRX(1);
	WPI_TalonSRX armStore = new WPI_TalonSRX(2);
	PIDController liftPID = new PIDController(0.001, 0, 0, liftEnc, pidStore);
	PIDController armPID = new PIDController(0.05, 0, 0, armEnc, armStore);
	PressureSensor pressure = new PressureSensor(0);
	CameraServer camera = CameraServer.getInstance();
	AHRS gyro = new AHRS(Port.kMXP);
	PIDController strPID = new PIDController(0.15, 0, 0, gyro, armStore);
	
	DriverStation matchInfo = DriverStation.getInstance(); //Object to get switch/scale colors
	
	String sides; //A string to store the switch and scale colors
	int position; //An integer to store the starting position
	char scalePos;
	char switchPos;
	AutonType autonMode; //Enumerator for the autonomous mode
	int step;
	boolean doOnce = true;
	boolean liftToggle = false;
	double time;
	final static double scaleNeutralHeight = 20000;
	final static double switchHeight = 3000; //TODO change this
	
	@Override
	public void robotInit() {
		pidStore.disable();
		cubeLift.getSensorCollection();
		camera.startAutomaticCapture("cam0", 0);
		compressor.start(); //Start compressor
		driver.addDefault(Tim, Tim);//Add Tim's profile to driver chooser
		driver.addObject(Spencer, Spencer); //Add Spencer's profile to driver chooser
		
		man.addDefault(Troy, Troy);//Add Troy's profile to manip. chooser
		man.addObject(Collin, Collin); //Add Collin's profile to manip. chooser
		man.addObject(Connor, Connor);
		
		//Put choosers on SmartDashboard
		SmartDashboard.putData("Drivers", driver);
		SmartDashboard.putData("Manipulators", man);
		
		mainDrive.setSafetyEnabled(false); //Disable safety
		
		liftPID.setOutputRange(-0.7, 0.7);
		armPID.setOutputRange(-0.5, 0.5);
		liftEnc.zero();
	}
	@Override
	public void autonomousInit() {
		strPID.setSetpoint(0);
		driveEnc.reset();
		step = 1; //set the auton step to step 1
		sides = matchInfo.getGameSpecificMessage(); //Get the switch and scale colors
		sides = "RRR";
		switchPos = sides.charAt(0);
		scalePos = sides.charAt(1);
		position = matchInfo.getLocation(); //Get the robot's position
		position = 1;
		if(position == 1) {
			if(sides == "LLL") {
				autonMode = AutonType.leftSwitch;
				liftPID.setSetpoint(switchHeight);
			}
			if(sides == "RRR") {
				autonMode = AutonType.straight;
			}
			if(sides == "LRL") {
				autonMode = AutonType.leftSwitch;
				liftPID.setSetpoint(switchHeight);
			}
			if(sides == "RLR") {
				autonMode = AutonType.leftScale;
				liftPID.setSetpoint(scaleNeutralHeight);
			}
		} else if(position == 2) {
			autonMode = AutonType.straight;
		} else if(position == 3) {
			if(sides == "LLL") {
				autonMode = AutonType.straight;
			}
			if(sides == "RRR") {
				autonMode = AutonType.rightSwitch;
				liftPID.setSetpoint(switchHeight);
			}
			if(sides == "LRL") {
				autonMode = AutonType.rightScale;
				liftPID.setSetpoint(scaleNeutralHeight);
			}
			if(sides == "RLR") {
				autonMode = AutonType.rightSwitch;
				liftPID.setSetpoint(switchHeight);
			}
		}
		
		if(position == 1 && switchPos == 'L') {//If we can go for the left switch...
			autonMode = AutonType.leftSwitch;
		} else if(position == 3 && switchPos == 'R') {//If we can go for the right switch
			autonMode = AutonType.rightSwitch;
		} else if(position == 1 && scalePos == 'L') {//If we can go for the left scale
			autonMode = AutonType.leftScale;
		} else if(position == 3 && scalePos == 'R') {//If we can go for the right scale
			autonMode = AutonType.rightScale;
		} else if(position == 2) {//If we are in position two
			autonMode = AutonType.straight;
		} else {//If none of those are true
			autonMode = AutonType.straight;
		}
		autonMode = AutonType.leftScale;
		liftPID.enable();
		armPID.enable();
		armEnc.reset();
		//driveEnc.setDistancePerPulse(1);
	}
	@Override
	public void autonomousPeriodic() {
		release.set(0.5);
		
		read();
		
		switch(autonMode) {
		case straight:
			straight(); //Drive straight for auton
			break;
		case leftSwitch:
			leftSwitch(); //Go to the left side of the switch 
			break;
		case rightSwitch:
			rightSwitch(); //Go to the right side of the switch
			break;
		case leftScale:
			leftScale(); //Go to the left side of the scale
			break;
		case rightScale:
			rightScale(); //Go to the right side of the scale
			break;
		}
		
	}
	
	@Override
	public void teleopPeriodic() {
		driverString = driver.getSelected();
		manString = man.getSelected();
		release.set(0.5);
		
		/*******************
		 * DRIVER PROFILES *
		 *******************/
		
		if(joy1.getRawButton(12)) {
			driveEnc.reset();
		}
		
		if(driverString == Tim) { //Tim profile
			double sense = -0.5 * joy1.getRawAxis(3) + 0.5;
			double y = Math.pow(joy1.getRawAxis(1), 1); //Double to store the joystick's y axis
			double rot = -Math.pow(joy1.getRawAxis(2), 1)/2; //Double to store the joystick's x axis
			if(Math.abs(y) >= 0.05 || Math.abs(rot) >= 0.05 && !joy1.getRawButton(1)) { //Thresholding function
				mainDrive.arcadeDrive(y * sense, rot * sense); //Arcade drive with the joystick's axis
			} else {
				mainDrive.arcadeDrive(0, 0); //Stop if value doesn't meet threshhold
			}
			
			if(joy1.getRawButton(2)) { //Press and hold button 2 for omni wheels
				omni.set(out);
			} else {
				omni.set(in);
			}
			
			if(joy1.getRawButton(3)) { //Press and hold button 3 for transmission
				shift.set(out);
			} else {
				shift.set(in);
			}
			
		} else { //Spencer profile
			double y = Math.pow(joy1.getRawAxis(1), 3); //Double to store the joystick's y axis
			double rot = -Math.pow(joy1.getRawAxis(2), 3)/1.5; //Double to store the joystick's x axis
			if(Math.abs(y) >= 0.05 || Math.abs(rot) >= 0.05 && !joy1.getRawButton(1)) { //Thresholding function
				mainDrive.arcadeDrive(y, rot); //Arcade drive with the joystick's axis
			} else {
				mainDrive.arcadeDrive(0, 0);
			}
			
			if(joy1.getRawButton(2)) { //Press and hold button 2 for omni wheels
				omni.set(out);
			} else {
				omni.set(in);
			}
			
			if(joy1.getRawButton(3)) { //Press and hold button 3 for transmission
				shift.set(out);
			} else {
				shift.set(in);
			}
		}
		
		
		/************************
		 * MANIPULATOR PROFILES *
		 ************************/
		
		if(manString == Troy) {
			if(liftEnc.get() >= 20000) {
				joy2.setRumble(RumbleType.kLeftRumble, 1);
				joy2.setRumble(RumbleType.kRightRumble, 1);
			} else {
				joy2.setRumble(RumbleType.kLeftRumble, 0);
				joy2.setRumble(RumbleType.kRightRumble, 0);
			}
			
			if(doOnce) {
				liftPID.enable();
				doOnce = false;
			}
			if(Math.abs(joy2.getRawAxis(1)) >= 0.1) { //Threshhold for cube lift speed
				liftPID.reset();
				cubeLift.set(joy2.getRawAxis(1));
				liftPID.setSetpoint(liftEnc.get());
				doOnce = true;
			} else if(joy2.getRawButtonReleased(1)) {
				liftPID.reset();
				doOnce = true;
				liftToggle = !liftToggle;
				if(liftToggle) {
					liftPID.setSetpoint(scaleNeutralHeight);
				} else {
					liftPID.setSetpoint(0);
				}
			} else {
				cubeLift.set(-liftPID.get());
			}
			
			if(Math.abs(joy2.getRawAxis(5)) >= 0.1) { //Threshhold for cube lift speed
				arm.set(joy2.getRawAxis(5));
				armPID.setSetpoint(armEnc.get());
				armPID.enable();
			} else {
				arm.set(armPID.get());
			}
			
			
			if(Math.abs(joy2.getRawAxis(2)) >= 0.25) { //If the left trigger is pulled...
				leftHolder.set(0.85); //Input cube
				rightHolder.set(-0.85);
			} else if(Math.abs(joy2.getRawAxis(3)) >= 0.25) { //If right trigger is pulled...
				leftHolder.set(-0.75);// Output cube
				rightHolder.set(0.75);
			} else if(joy2.getRawButton(5)) { //If left bumper is pressed...
				leftHolder.set(-0.75); // Rotate cube
				rightHolder.set(-0.75);
			} else if(joy2.getRawButton(6)) { //If right bumper is pressed...
				leftHolder.set(0.75); // Rotate cube
				rightHolder.set(0.75);
			} else { //If nothing is pressed...
				leftHolder.set(0); //Stop cube motors
				rightHolder.set(0);
			}
		} else if(manString == Collin){
			if(liftEnc.get() >= 20000) {
				joy2.setRumble(RumbleType.kLeftRumble, 1);
				joy2.setRumble(RumbleType.kRightRumble, 1);
			} else {
				joy2.setRumble(RumbleType.kLeftRumble, 0);
				joy2.setRumble(RumbleType.kRightRumble, 0);
			}
			
			if(doOnce) {
				liftPID.enable();
				doOnce = false;
			}
			if(Math.abs(joy2.getRawAxis(1)) >= 0.08) { //Threshhold for cube lift speed
				liftPID.reset();
				cubeLift.set(joy2.getRawAxis(1));
				liftPID.setSetpoint(liftEnc.get());
				doOnce = true;
			} else if(joy2.getPOV() == 0) { //If the D-pad is up...
				doOnce = true;
				liftPID.reset();
				liftPID.setSetpoint(scaleNeutralHeight);
			} else if(joy2.getPOV() == 180) { //If the D-pad is down...
				doOnce = true;
				liftPID.reset();
				liftPID.setSetpoint(0);
			} else {
				cubeLift.set(-liftPID.get());
			}
			
			if(Math.abs(joy2.getRawAxis(5)) >= 0.05) { //Variable input/output cube
				leftHolder.set(joy2.getRawAxis(5));
				rightHolder.set(-joy2.getRawAxis(5));
			} else if(Math.abs(joy2.getRawAxis(2)) >= 0.05) { //Variable rotate cube left
				leftHolder.set(joy2.getRawAxis(2));
				rightHolder.set(joy2.getRawAxis(2));
			} else if(Math.abs(joy2.getRawAxis(3)) >= 0.05) { //Variable rotate cube right
				leftHolder.set(-joy2.getRawAxis(3));
				rightHolder.set(-joy2.getRawAxis(3));
			} else { //Else, stop
				leftHolder.set(0);
				rightHolder.set(0);
			}
		} else {
			
			if(joy2.getRawButton(1)) {
				cubeLift.set(-liftPID.get());
			} else {
				cubeLift.set(0);
			}
			
			if(joy2.getRawButton(2)) {
				liftEnc.zero();
				liftPID.reset();
			}
		}
		if(joy2.getRawButton(10)) {
			liftEnc.zero();
			liftPID.reset();
			armEnc.reset();
		}
		read();
	}
	
	void read() {//25428.0
		SmartDashboard.putNumber("Drive distance", liftEnc.get());
		SmartDashboard.putNumber("PID", liftPID.get());
		SmartDashboard.putNumber("Motor", cubeLift.get());
		SmartDashboard.putNumber("POV", joy2.getPOV());
		SmartDashboard.putNumber("COntroller", joy2.getRawAxis(1));
		if(pressure.get() >= 30) {
			SmartDashboard.putBoolean("usable pressure", true);
		} else {
			SmartDashboard.putBoolean("usable pressure", false);
		}
		SmartDashboard.putNumber("Pressure", pressure.get());
		SmartDashboard.putNumber("Arm Encoder", armEnc.get());
		SmartDashboard.putNumber("Arm PID", armPID.get());
		SmartDashboard.putNumber("Axis 5", joy2.getRawAxis(5));
		SmartDashboard.putBoolean("g", driveEnc.g());
		SmartDashboard.putNumber("STRAIGHT PID", strPID.get());
		SmartDashboard.putNumber("Gyro", gyro.getAngle());
		SmartDashboard.putNumber("Setpoint", liftPID.getSetpoint());
		SmartDashboard.putNumber("Touchless ticks", driveEnc.get());
	}
	
	@Override
	public void testPeriodic() {
	}
	
	enum AutonType {
		rightScale, leftScale, rightSwitch, leftSwitch, straight
	}
	
	void straight() {
		double distance = driveEnc.get();
		if(distance < 120) {
			mainDrive.arcadeDrive(-0.75, 0);
		} else {
			mainDrive.arcadeDrive(0, 0);
		}
	}
	
	void rightScale() {
		switch(step) {
		case 1:
			
			if(driveEnc.get() < 300) {
				mainDrive.arcadeDrive(0.75, strPID.get());
			} else {
				step = 2;
				strPID.disable();
			}
			break;
		case 2:
			if(gyro.getAngle() > -90) {
				mainDrive.arcadeDrive(0, -0.3);
			} else {
				step = 3;
			}
			break;
		case 3:
			cubeLift.set(-liftPID.get());
			if(driveEnc.get() < 12) {
				mainDrive.arcadeDrive(0.2, 0);
				cubeLift.set(0.5);
			} else {
				step = 4;
				cubeLift.set(0);
				time = Timer.getMatchTime();
			}
			break;
		case 4:
			cubeLift.set(-liftPID.get());
			if(time - Timer.getMatchTime() <= 1.0) {
				leftHolder.set (-1);
				rightHolder.set(1);
			} else {
				step = 5;
				leftHolder.set(0);
				rightHolder.set(0);
			}
			break;
		}
	}
	
	void leftSwitch() {
		strPID.enable();
		shift.set(out);
		switch(step) {
		case 1:
			liftPID.setSetpoint(10000);
			armPID.setSetpoint(75);
			armPID.enable();
			liftPID.enable();
			cubeLift.set(-liftPID.get());
			if(driveEnc.get() < 149) {
				mainDrive.arcadeDrive(-0.75, -strPID.get());
			} else {
				mainDrive.arcadeDrive(0, 0);
				step = 2;
			}
			break;
		case 2:
			cubeLift.set(-liftPID.get());
			arm.set(armPID.get());
			if(gyro.getAngle() < 80) {
				mainDrive.arcadeDrive(0, -0.6);
			} else {
				step = 3;
				driveEnc.reset();
				mainDrive.arcadeDrive(0, 0);
			}
			break;
		case 3:
			cubeLift.set(-liftPID.get());
			arm.set(armPID.get());
			if(driveEnc.get() < 9) {
				mainDrive.arcadeDrive(-0.5, 0);
			} else {
				step = 4;
				mainDrive.arcadeDrive(0, 0);
				time = Timer.getMatchTime();
			}
			break;
		case 4:
			cubeLift.set(-liftPID.get());
			arm.set(armPID.get());
			if(Math.abs(time - Timer.getMatchTime()) <= 3.0) {
				leftHolder.set(-0.5);
				rightHolder.set(-0.5);
			} else {
				step = 5;
				leftHolder.set(0);
				rightHolder.set(0);
			}
			break;
		}
	}
	
	void leftScale() {//TODO
		strPID.enable();
		shift.set(out);
		switch(step) {
		case 1:
			liftPID.setSetpoint(15000);
			armPID.setSetpoint(75);
			armPID.enable();
			liftPID.enable();
			cubeLift.set(-liftPID.get());
			if(driveEnc.get() < 252) {
				mainDrive.arcadeDrive(-0.75, -strPID.get());
			} else {
				mainDrive.arcadeDrive(0, 0);
				step = 2;
				liftPID.setSetpoint(20000);
			}
			break;
		case 2:
			if(liftEnc.get() < 24000) {
				cubeLift.set(-liftPID.get());
			} else {
				cubeLift.set(0);
				liftPID.setSetpoint(10000);
			}
			arm.set(armPID.get());
			if(gyro.getAngle() < 35) {
				mainDrive.arcadeDrive(0, -0.4);
			} else {
				step = 10;
				driveEnc.reset();
				mainDrive.arcadeDrive(0, 0);
			}
			break;
		}
	}
	void rightSwitch() {
		strPID.enable();
		shift.set(out);
		switch(step) {
		case 1:
			liftPID.setSetpoint(10000);
			armPID.setSetpoint(75);
			armPID.enable();
			liftPID.enable();
			cubeLift.set(-liftPID.get());
			if(driveEnc.get() < 149) {
				mainDrive.arcadeDrive(-0.75, -strPID.get());
			} else {
				mainDrive.arcadeDrive(0, 0);
				step = 2;
			}
			break;
		case 2:
			cubeLift.set(-liftPID.get());
			arm.set(armPID.get());
			if(gyro.getAngle() > -80) {
				mainDrive.arcadeDrive(0, 0.6);
			} else {
				step = 3;
				driveEnc.reset();
				mainDrive.arcadeDrive(0, 0);
			}
			break;
		case 3:
			cubeLift.set(-liftPID.get());
			arm.set(armPID.get());
			if(driveEnc.get() < 9) {
				mainDrive.arcadeDrive(-0.5, 0);
			} else {
				step = 4;
				mainDrive.arcadeDrive(0, 0);
				time = Timer.getMatchTime();
			}
			break;
		case 4:
			cubeLift.set(-liftPID.get());
			arm.set(armPID.get());
			if(Math.abs(time - Timer.getMatchTime()) <= 3.0) {
				leftHolder.set(-0.5);
				rightHolder.set(-0.5);
			} else {
				step = 5;
				leftHolder.set(0);
				rightHolder.set(0);
			}
			break;
		}
	}
	
	
}