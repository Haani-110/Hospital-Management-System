package com.hospital;

import com.hospital.config.DatabaseConnection;
import com.hospital.config.DatabaseInitializer;
import com.hospital.controller.AppointmentController;
import com.hospital.controller.DashboardController;
import com.hospital.controller.DepartmentController;
import com.hospital.controller.DoctorController;
import com.hospital.controller.LoginController;
import com.hospital.controller.PatientController;
import com.hospital.controller.UserManagementController;
import com.hospital.dao.AppointmentDao;
import com.hospital.dao.AppointmentDaoImpl;
import com.hospital.dao.DepartmentDao;
import com.hospital.dao.DepartmentDaoImpl;
import com.hospital.dao.DoctorDao;
import com.hospital.dao.DoctorDaoImpl;
import com.hospital.dao.PatientDao;
import com.hospital.dao.PatientDaoImpl;
import com.hospital.dao.UserDao;
import com.hospital.dao.UserDaoImpl;
import com.hospital.service.AppointmentService;
import com.hospital.service.AuthService;
import com.hospital.service.DepartmentService;
import com.hospital.service.DoctorService;
import com.hospital.service.PatientService;
import com.hospital.service.UserService;
import com.hospital.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point for the Hospital Management System.
 */
public class Main extends Application {

    private AuthService authService;
    private DepartmentService departmentService;
    private UserService userService;
    private DoctorService doctorService;
    private PatientService patientService;
    private AppointmentService appointmentService;

    @Override
    public void init() {
        DatabaseConnection dbConnection = new DatabaseConnection();
        new DatabaseInitializer(dbConnection).initialize();

        UserDao userDao = new UserDaoImpl(dbConnection);
        authService = new AuthService(userDao);
        authService.seedDemoUsersIfEmpty();

        DepartmentDao departmentDao = new DepartmentDaoImpl(dbConnection);
        departmentService = new DepartmentService(departmentDao);

        userService = new UserService(userDao);

        DoctorDao doctorDao = new DoctorDaoImpl(dbConnection);
        doctorService = new DoctorService(doctorDao, departmentDao);

        PatientDao patientDao = new PatientDaoImpl(dbConnection);
        patientService = new PatientService(patientDao);

        AppointmentDao appointmentDao = new AppointmentDaoImpl(dbConnection);
        appointmentService = new AppointmentService(appointmentDao, patientDao, doctorDao);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hospital Management System");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        SceneManager sceneManager = new SceneManager(primaryStage);

        LoginController loginController = new LoginController(authService, sceneManager);
        DashboardController dashboardController = new DashboardController(authService, sceneManager);
        DepartmentController departmentController = new DepartmentController(departmentService, sceneManager);
        UserManagementController userController = new UserManagementController(userService, sceneManager);
        DoctorController doctorController = new DoctorController(doctorService, departmentService, sceneManager);
        PatientController patientController = new PatientController(patientService, sceneManager);
        AppointmentController appointmentController = new AppointmentController(
                appointmentService, patientService, doctorService, sceneManager);

        // --- Navigation wiring ---

        Runnable showDashboard = () -> {
            dashboardController.setCurrentUser(null);
            sceneManager.show(dashboardController.buildScene());
        };
        Runnable showDepartments = () -> sceneManager.show(departmentController.buildScene());
        Runnable showUsers = () -> sceneManager.show(userController.buildScene());
        Runnable showDoctors = () -> sceneManager.show(doctorController.buildScene());
        Runnable showPatients = () -> sceneManager.show(patientController.buildScene());
        Runnable showAppointments = () -> sceneManager.show(appointmentController.buildScene());

        loginController.setOnLoginSuccess(user -> {
            dashboardController.setCurrentUser(user);
            sceneManager.show(dashboardController.buildScene());
        });
        dashboardController.setOnLogout(() -> sceneManager.show(loginController.buildScene()));
        dashboardController.setOnOpenDepartments(showDepartments);
        dashboardController.setOnOpenUserManagement(showUsers);
        dashboardController.setOnOpenDoctors(showDoctors);
        dashboardController.setOnOpenPatients(showPatients);
        dashboardController.setOnOpenAppointments(showAppointments);

        departmentController.setOnBackToDashboard(showDashboard);
        departmentController.setOnOpenUserManagement(showUsers);
        departmentController.setOnOpenDoctors(showDoctors);
        departmentController.setOnOpenPatients(showPatients);
        departmentController.setOnOpenAppointments(showAppointments);

        userController.setOnBackToDashboard(showDashboard);
        userController.setOnOpenDepartments(showDepartments);
        userController.setOnOpenDoctors(showDoctors);
        userController.setOnOpenPatients(showPatients);
        userController.setOnOpenAppointments(showAppointments);

        doctorController.setOnBackToDashboard(showDashboard);
        doctorController.setOnOpenDepartments(showDepartments);
        doctorController.setOnOpenUserManagement(showUsers);
        doctorController.setOnOpenPatients(showPatients);
        doctorController.setOnOpenAppointments(showAppointments);

        patientController.setOnBackToDashboard(showDashboard);
        patientController.setOnOpenDepartments(showDepartments);
        patientController.setOnOpenDoctors(showDoctors);
        patientController.setOnOpenUserManagement(showUsers);
        patientController.setOnOpenAppointments(showAppointments);

        appointmentController.setOnBackToDashboard(showDashboard);
        appointmentController.setOnOpenDepartments(showDepartments);
        appointmentController.setOnOpenDoctors(showDoctors);
        appointmentController.setOnOpenPatients(showPatients);
        appointmentController.setOnOpenUserManagement(showUsers);

        sceneManager.show(loginController.buildScene());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
