import java.util.Map;
import java.util.WeakHashMap;

public privileged aspect MergeEmployee {

private personnel.Employee personnelEmployee;
private payroll.Employee payrollEmployee;

private Map<personnel.Employee, payroll.Employee> personnelTopayrollMapping = new WeakHashMap<>();
private Map<payroll.Employee, personnel.Employee> payrollTopersonnelMapping = new WeakHashMap<>();

pointcut personnelEmployeeConstructor(String name) :
    call(personnel.Employee.new(String)) &&
    args(name) &&
    !within(MergeEmployee);

pointcut payrollEmployeeConstructor(String name) :
    call(payroll.Employee.new(String)) &&
    args(name) &&
    !within(MergeEmployee);

before(String name) : personnelEmployeeConstructor(name) {
    personnelEmployee = (personnel.Employee) thisJoinPoint.getTarget();
}
after(String name) : personnelEmployeeConstructor(name) {
    payrollTopersonnelMapping.put(payrollEmployee, new personnel.Employee(name));
}

before(String name) : payrollEmployeeConstructor(name) {
    payrollEmployee = (payroll.Employee) thisJoinPoint.getTarget();
}
after(String name) : payrollEmployeeConstructor(name) {
    personnelTopayrollMapping.put(personnelEmployee, new payroll.Employee(name));
}

// Replace payroll.Employee.name with personnel.Employee.name
String around(): get(String payroll.Employee.name) && !within(MergeEmployee) {
    payroll.Employee payrollEmployee = (payroll.Employee) thisJoinPoint.getTarget();
    personnel.Employee personnelEmployee = payrollTopersonnelMapping.get(payrollEmployee);
    return personnelEmployee.name;
}
void around(String newval): set(String payroll.Employee.name) && args(newval) && !within(MergeEmployee) {
    payroll.Employee payrollEmployee = (payroll.Employee) thisJoinPoint.getTarget();
    personnel.Employee personnelEmployee = payrollTopersonnelMapping.get(payrollEmployee);
    personnelEmployee.name = newval;
}

}
