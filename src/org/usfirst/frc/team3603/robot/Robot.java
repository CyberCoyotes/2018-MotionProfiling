package org.usfirst.frc.team3603.robot;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;

import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.CounterBase.EncodingType;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
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
	
	Compressor compressor = new Compressor();
	DoubleSolenoid omni = new DoubleSolenoid(0, 1); //Omni solenoid
	DoubleSolenoid shift = new DoubleSolenoid(2, 3);//Transmission solenoid
	
	Joystick joy1 = new Joystick(0); //Large twist-axis joystick
	Joystick joy2 = new Joystick(1); //Xbox controller
	ADXRS450_Gyro gyro = new ADXRS450_Gyro(); //Gyro needs switched
	MyEncoder liftEnc = new MyEncoder(cubeLift, false, 1.0); //Encoder for the cube lift
	Encoder armEnc = new Encoder(2, 3, false, EncodingType.k2X);
	WPI_TalonSRX pidStore = new WPI_TalonSRX(1);
	WPI_TalonSRX pidStore2 = new WPI_TalonSRX(2);
	PIDController liftPID = new PIDController(0.0005, 0, 0, liftEnc, pidStore);
	PIDController armPID = new PIDController(0.0005, 0, 0, armEnc, pidStore2);
	
	PressureSensor pressure = new PressureSensor(0);
	//Encoder driveEnc = new Encoder(0, 0, true, EncodingType.k2X); //Encoder for distance driven
	double mult = 1.0; //Multiplier for driveEnc TODO multiplier
	
	DriverStation matchInfo = DriverStation.getInstance(); //Object to get switch/scale colors
	
	String sides; //A string to store the switch and scale colors
	int position; //An integer to store the starting position
	char scalePos;
	char switchPos;
	AutonType autonMode; //Enumerator for the autonomous mode
	int step;
	boolean doOnce = true;
	boolean armEnable = true;
	boolean liftToggle = false;
	boolean did = false;
	final static double scaleNeutralHeight = 20000; //TODO change this
	
	@Override
	public void robotInit() {
		pidStore.disable();
		pidStore2.disable();
		cubeLift.getSensorCollection();
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
		
		liftPID.setSetpoint(0);
		liftPID.setOutputRange(-0.7, 0.7);
		liftEnc.zero();
		armPID.setSetpoint(0);
		armPID.setOutputRange(-0.5,  0.5);
	}
	@Override
	public void autonomousInit() {
		step = 1; //set the auton step to step 1
		sides = matchInfo.getGameSpecificMessage(); //Get the switch and scale colors
		switchPos = sides.charAt(0);
		scalePos = sides.charAt(1);
		position = matchInfo.getLocation(); //Get the robot's position
		
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
		
		//driveEnc.setDistancePerPulse(1);
	}
	@Override
	public void autonomousPeriodic() {
		/*
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
		*/
	}
	
	@Override
	public void teleopPeriodic() {
		driverString = driver.getSelected();
		manString = man.getSelected();
		
		
		/*******************
		 * DRIVER PROFILES *
		 *******************/
		
		if(driverString == Tim) { //Tim profile
			double y = Math.pow(joy1.getRawAxis(1), 3); //Double to store the joystick's y axis
			double rot = -Math.pow(joy1.getRawAxis(2), 3)/1.5; //Double to store the joystick's x axis
			if(Math.abs(y) >= 0.05 || Math.abs(rot) >= 0.05 && !joy1.getRawButton(1)) { //Thresholding function
				mainDrive.arcadeDrive(y, rot); //Arcade drive with the joystick's axis
			} else {
				mainDrive.arcadeDrive(0, 0); //Stop if value doesn't meet threshhold
			}
			
			if(joy1.getRawButton(2)) { //Press and hold button 2 for omni wheels
				omni.set(out);
			} else {
				omni.set(in);
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
			if(Math.abs(joy2.getRawAxis(1)) >= 0.08) { //If the left joystick is being moved...
				liftPID.reset(); //Reset (delete previous values of) the PID
				cubeLift.set(joy2.getRawAxis(1)); //Set the lift speed to the joystick
				liftPID.setSetpoint(liftEnc.get()); //Read the new position so when the joystick is released, the position will be held
				doOnce = true; //Enable the PID 
			} else if(joy2.getRawButton(1)) {
				liftPID.reset(); //Reset (delete previous values of) the PID
				doOnce = true; //Enable the PID
				liftToggle = !liftToggle; //Toggle if the lift should be up or down
				if(liftToggle) { //If true...
					liftPID.setSetpoint(scaleNeutralHeight); //Raise the lift
				} else { //If false...
					liftPID.setSetpoint(0); //Lower the lift
				}
				while(joy2.getRawButton(1)) {} //Wait for the button to be released
			} else if(doOnce) { //If the enable boolean is true...
				liftPID.enable(); //Enable PID
				doOnce = false; //Set the boolean to false so it won't enable again
			} else { //If nothing is being pressed...
				cubeLift.set(-liftPID.get()); //Set the cube lift speed to the opposite of the PID value
			}
			
			
			if(Math.abs(joy2.getRawAxis(2)) >= 0.25) { //If the left trigger is pulled...
				leftHolder.set(0.75); //Input cube
				rightHolder.set(-0.75);
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
		} else if(manString == Collin){ //Collin's profile
			if(Math.abs(joy2.getRawAxis(1)) >= 0.08) { //Iff the left joystick is moved...
				liftPID.reset(); //Reset (delete previous values of) the PID
				cubeLift.set(joy2.getRawAxis(1)); //Set the lift speed to the joystick
				liftPID.setSetpoint(liftEnc.get()); //Read the new position so when the joystick is released, the position will be held
				doOnce = true; //Enable the PID
			} else if(joy2.getPOV() == 0) { //If the D-pad is up...
				doOnce = true; //Enable the PID
				liftPID.reset(); //Reset the PID
				liftPID.setSetpoint(scaleNeutralHeight); //Set the lift to scale neutral height
			} else if(joy2.getPOV() == 180) { //If the D-pad is down...
				doOnce = true; //Enable the PID
				liftPID.reset(); //Reset the PID
				liftPID.setSetpoint(0); //Set the setpoint to ground
			} else if(doOnce) { //If the enable boolean is true...
				liftPID.enable(); //Enable the PID
				doOnce = false; //Set the enable boolean to false
			} else { //If nothing is being pressed...
				cubeLift.set(-liftPID.get()); //Set the lift speed to the opposite of the PID speed
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
		} else { //Connor's profile/ EXPERIMENTAL
			/**
			 * If you need to test the cube arm thing, this
			 * profile is the once with the code. If the
			 * PID turns the wrong way, put a negative in
			 * line 325. It's also marked with a comment
			 * where it should go.
			 */
			
			if(doOnce) {
				liftPID.enable();
				doOnce = false;
			}
			if(Math.abs(joy2.getRawAxis(1)) >= 0.08) { //Threshhold for cube lift speed
				liftPID.reset();
				cubeLift.set(joy2.getRawAxis(1));
				liftPID.setSetpoint(liftEnc.get());
				doOnce = true;
			} else if(joy2.getRawButton(1)) {
				liftPID.reset();
				doOnce = true;
				liftToggle = !liftToggle;
				if(liftToggle) {
					liftPID.setSetpoint(scaleNeutralHeight);
				} else {
					liftPID.setSetpoint(0);
				}
				while(joy2.getRawButton(1)) {}
			} else {
				cubeLift.set(-liftPID.get());
			}
			
			if(armEnable) {
				armPID.enable();
				armEnable = false;
			}
			if(Math.abs(joy2.getRawAxis(5)) >= 0.08) {
				armPID.reset();
				arm.set(joy2.getRawAxis(5));
				armPID.setSetpoint(armEnc.get());
				armEnable = true;
			} else {
				arm.set(/*PUT A NEGATIVE AFTER THIS COMMENT IF THE ARM IS BACKWARDS->*/armPID.get());
			}
			
			if(Math.abs(joy2.getRawAxis(2)) >= 0.25) { //If the left trigger is pulled...
				leftHolder.set(0.75); //Input cube
				rightHolder.set(-0.75);
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
		}
		if(joy2.getRawButton(10)) {
			liftEnc.zero();
			liftPID.reset();
		}
		read();
	}
	
	void read() {//25428.0
		SmartDashboard.putNumber("Drive distance", liftEnc.get());
		SmartDashboard.putNumber("PID", liftPID.get());
		SmartDashboard.putNumber("Motor", cubeLift.get());
		SmartDashboard.putNumber("POV", joy2.getPOV());
		if(pressure.get() >= 30) {
			SmartDashboard.putBoolean("usable pressure", true);
		} else {
			SmartDashboard.putBoolean("usable pressure", false);
		}
		SmartDashboard.putNumber("Pressure", pressure.get());
	}
	
	@Override
	public void testPeriodic() {
		/*
		if(Math.abs(joy2.getRawAxis(1)) >= 0.05) { //Threshhold for cube lift speed
			liftPID.disable();
			liftPID.reset();
			did = false;
			cubeLift.set(joy2.getRawAxis(2));
			liftPID.setSetpoint(liftEnc.get());
			liftPID.enable();
		} else if(joy1.getRawButton(1)) {
			liftPID.disable();
			liftPID.reset();
			liftToggle = !liftToggle;
			if(liftToggle) {
				liftPID.setSetpoint(scaleNeutralHeight);
			} else {
				liftPID.setSetpoint(0);
			}
			while(joy1.getRawButton(1)) {}
			liftPID.enable();
		} else if(!did) {
			liftPID.enable();
			did = true;
		}
		
		
		if(Math.abs(joy2.getRawAxis(2)) >= 0.25) { //If the left trigger is pulled...
			leftHolder.set(0.75); //Input cube
			rightHolder.set(-0.75);
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
		*/
	}
	
	enum AutonType {
		rightScale, leftScale, rightSwitch, leftSwitch, straight
	}
	/*
	void straight() {
		double distance = driveEnc.get();
		if(distance < 120) {
			drive(0.75);
		} else {
			drive(0);
		}
	}
	
	void rightScale() {
		switch(step) {
		case 1:
			if(driveEnc.get() < 300) {
				mainDrive.arcadeDrive(1, 0);
			} else {
				step = 2;
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
	void leftScale() {
		switch(step) {
		case 1:
			if(driveEnc.get() < 300) {
				mainDrive.arcadeDrive(1, 0);
			} else {
				step = 2;
			}
			break;
		case 2:
			if(gyro.getAngle() < 90) {
				mainDrive.arcadeDrive(0, 0.3);
			} else {
				step = 3;
			}
			break;
		case 3:
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
	void rightSwitch() {
		switch(step) {
		case 1:
			if(driveEnc.get() < 168) {
				mainDrive.arcadeDrive(1, 0);
			} else {
				step = 2;
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
		switch(step) {
		case 1:
			if(driveEnc.get() < 168) {
				mainDrive.arcadeDrive(1, 0);
			} else {
				step = 2;
			}
			break;
		case 2:
			if(gyro.getAngle() < 90) {
				mainDrive.arcadeDrive(0, 0.3);
			} else {
				step = 3;
			}
			break;
		case 3:
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
	void drive(double speed) {
		mainDrive.arcadeDrive(speed, 0);
	}
	void turn(double speed) {
		mainDrive.arcadeDrive(0, speed);
	}
	void place() {
	}
	*/
}
