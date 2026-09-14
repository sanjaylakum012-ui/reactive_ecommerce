package com.rahulsmgv.cbs.customer.application.service;

import com.rahulsmgv.cbs.customer.application.dto.CreateCustomerCommand;
import com.rahulsmgv.cbs.customer.application.dto.CustomerResponse;
import com.rahulsmgv.cbs.customer.application.dto.UpdateCustomerCommand;
import com.rahulsmgv.cbs.customer.application.port.CustomerRepository;
import com.rahulsmgv.cbs.customer.application.port.CustomerIdGenerator;
import com.rahulsmgv.cbs.customer.domain.model.Customer;
import com.rahulsmgv.cbs.customer.domain.valueobject.Address;
import com.rahulsmgv.cbs.customer.domain.valueobject.CustomerId;
import com.rahulsmgv.cbs.customer.domain.valueobject.CustomerName;
import com.rahulsmgv.cbs.customer.domain.valueobject.EmailAddress;
import com.rahulsmgv.cbs.customer.domain.valueobject.MobileNumber;
import com.rahulsmgv.cbs.customer.exception.DuplicateCustomerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service responsible for executing
 * Customer-related use cases.
 *
 * @Service tells Spring to create and manage this
 *          class as a Spring bean.
 */
@Service
public class CustomerApplicationService {

        private static final Logger log = LoggerFactory.getLogger(CustomerApplicationService.class);

        private final CustomerRepository customerRepository;
        private final CustomerIdGenerator customerIdGenerator;

        /**
         * Constructor injection.
         *
         * Spring automatically injects the implementation
         * of CustomerRepository.
         */
        public CustomerApplicationService(
                        CustomerRepository customerRepository,
                        CustomerIdGenerator customerIdGenerator) {
                this.customerRepository = customerRepository;
                this.customerIdGenerator = customerIdGenerator;
        }

        /**
         * Create-customer flow:
         * 1. Validate duplicate business keys
         * 2. Build the aggregate from the command
         * 3. Save the aggregate and return the API response
         */
        public CustomerResponse createCustomer(
                        CreateCustomerCommand command) {

                log.info(
                                "Execution step started: createCustomer for email={}, mobile={}",
                                command.emailAddress(),
                                command.mobileNumber());

                // Step 1: Guard against duplicate email address.
                boolean duplicateEmail = customerRepository.existsByEmailAddress(command.emailAddress());
                log.info(
                                "Duplicate email check for email={}, exists={}",
                                command.emailAddress(),
                                duplicateEmail);
                if (duplicateEmail) {
                        throw new DuplicateCustomerException(
                                        "Customer already exists with email address: "
                                                        + command.emailAddress());
                }

                // Step 2: Guard against duplicate mobile number.
                boolean duplicateMobile = customerRepository.existsByMobileNumber(command.mobileNumber());
                log.info(
                                "Duplicate mobile check for mobile={}, exists={}",
                                command.mobileNumber(),
                                duplicateMobile);
                if (duplicateMobile) {
                        throw new DuplicateCustomerException(
                                        "Customer already exists with mobile number: "
                                                        + command.mobileNumber());
                }

                // Step 3: Create the aggregate using the domain model's factory.
                Customer customer = Customer.create(
                                customerIdGenerator.nextCustomerId(),
                                CustomerName.of(command.name()),
                                command.customerType(),
                                EmailAddress.of(command.emailAddress()),
                                MobileNumber.of(command.mobileNumber()),
                                Address.of(
                                                command.addressLine1(),
                                                command.addressLine2(),
                                                command.city(),
                                                command.state(),
                                                command.postalCode(),
                                                command.country()));

                // Step 4: Persist the customer and log the saved identity.
                Customer savedCustomer = customerRepository.save(customer);
                log.info(
                                "Execution step completed: createCustomer for customerId={}, status={}",
                                savedCustomer.customerId().value(),
                                savedCustomer.status());

                return toResponse(savedCustomer);
        }

        public CustomerResponse getCustomer(Long customerId) {

                log.info("Execution step started: getCustomer for customerId={}", customerId);

                CustomerId id = CustomerId.of(customerId);

                // Retrieve the customer aggregate using the primary identifier.
                Customer customer = customerRepository
                                .findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Customer not found: " + customerId));

                log.info(
                                "Execution step completed: getCustomer for customerId={}, status={}",
                                customerId,
                                customer.status());

                return toResponse(customer);
        }

        /**
         * Updates an existing customer.
         *
         * Responsibilities:
         * - Load the current aggregate
         * - Validate identity conflicts against the new email/mobile
         * - Update domain state through aggregate methods
         * - Save the aggregate and return the response
         */
        public CustomerResponse updateCustomer(Long customerId, UpdateCustomerCommand command) {

                log.info(
                                "Execution step started: updateCustomer for customerId={}, email={}",
                                customerId,
                                command.emailAddress());

                // Step 1: Resolve the customer aggregate from the repository.
                CustomerId id = CustomerId.of(customerId);
                Customer customer = customerRepository
                                .findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Customer not found: " + customerId));

                // Step 2: Ensure no other customer uses the requested email.
                boolean emailAlreadyTaken = !customer.emailAddress().value()
                                .equalsIgnoreCase(command.emailAddress())
                                && customerRepository.existsByEmailAddress(command.emailAddress());
                log.info(
                                "Email conflict validation for customerId={}, email={}, conflict={}",
                                customerId,
                                command.emailAddress(),
                                emailAlreadyTaken);
                if (emailAlreadyTaken) {
                        throw new DuplicateCustomerException(
                                        "Customer already exists with email address: " + command.emailAddress());
                }

                // Step 3: Ensure no other customer uses the requested mobile number.
                boolean mobileAlreadyTaken = !customer.mobileNumber().value()
                                .equals(command.mobileNumber())
                                && customerRepository.existsByMobileNumber(command.mobileNumber());
                log.info(
                                "Mobile conflict validation for customerId={}, mobile={}, conflict={}",
                                customerId,
                                command.mobileNumber(),
                                mobileAlreadyTaken);
                if (mobileAlreadyTaken) {
                        throw new DuplicateCustomerException(
                                        "Customer already exists with mobile number: " + command.mobileNumber());
                }

                // Step 4: Update the domain aggregate by invoking its lifecycle methods.
                customer.updateName(CustomerName.of(command.name()));
                customer.updateContactInformation(
                                EmailAddress.of(command.emailAddress()),
                                MobileNumber.of(command.mobileNumber()));
                customer.updateAddress(
                                Address.of(
                                                command.addressLine1(),
                                                command.addressLine2(),
                                                command.city(),
                                                command.state(),
                                                command.postalCode(),
                                                command.country()));

                // Step 5: Persist the updated aggregate state.
                Customer updatedCustomer = customerRepository.save(customer);
                log.info(
                                "Execution step completed: updateCustomer for customerId={}, status={}",
                                updatedCustomer.customerId().value(),
                                updatedCustomer.status());

                return toResponse(updatedCustomer);
        }

        /**
         * Deletes an existing customer.
         *
         * The application service first retrieves the customer
         * from the repository and then asks the repository to
         * delete the aggregate.
         */
        public void deleteCustomer(Long customerId) {

                log.info("Execution step started: deleteCustomer for customerId={}", customerId);

                CustomerId id = CustomerId.of(customerId);

                // Step 1: Load the aggregate to confirm it exists.
                Customer customer = customerRepository
                                .findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Customer not found: " + customerId));

                // Step 2: Delete the aggregate from the persistence layer.
                customerRepository.delete(customer);

                log.info(
                                "Execution step completed: deleteCustomer for customerId={}",
                                customerId);
        }

        private CustomerResponse toResponse(
                        Customer customer) {

                // Convert the aggregate to the API response contract.
                return new CustomerResponse(
                                customer.customerId().value(),
                                customer.name().value(),
                                customer.customerType(),
                                customer.status(),
                                customer.emailAddress().value(),
                                customer.mobileNumber().value(),
                                customer.address().addressLine1(),
                                customer.address().addressLine2(),
                                customer.address().city(),
                                customer.address().state(),
                                customer.address().postalCode(),
                                customer.address().country(),
                                customer.createdAt(),
                                customer.updatedAt());
        }

        /**
         * Activates a customer.
         *
         * The application service coordinates the use case,
         * while the Customer domain aggregate owns the
         * actual lifecycle transition rules.
         */
        public CustomerResponse activateCustomer(Long customerId) {

                log.info("Execution step started: activateCustomer for customerId={}", customerId);

                Customer customer = customerRepository
                                .findById(CustomerId.of(customerId))
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Customer not found: " + customerId));

                // Execute the domain validation and state transition.
                customer.activate();

                Customer savedCustomer = customerRepository.save(customer);

                log.info(
                                "Execution step completed: activateCustomer for customerId={}, status={}",
                                customerId,
                                savedCustomer.status());

                return toResponse(savedCustomer);
        }

        /**
         * Suspends a customer.
         */
        public CustomerResponse suspendCustomer(Long customerId) {

                log.info("Execution step started: suspendCustomer for customerId={}", customerId);

                Customer customer = customerRepository
                                .findById(CustomerId.of(customerId))
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Customer not found: " + customerId));

                customer.suspend();

                Customer savedCustomer = customerRepository.save(customer);

                log.info(
                                "Execution step completed: suspendCustomer for customerId={}, status={}",
                                customerId,
                                savedCustomer.status());

                return toResponse(savedCustomer);
        }

        /**
         * Blocks a customer.
         */
        public CustomerResponse blockCustomer(Long customerId) {

                log.info("Execution step started: blockCustomer for customerId={}", customerId);

                Customer customer = customerRepository
                                .findById(CustomerId.of(customerId))
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Customer not found: " + customerId));

                customer.block();

                Customer savedCustomer = customerRepository.save(customer);

                log.info(
                                "Execution step completed: blockCustomer for customerId={}, status={}",
                                customerId,
                                savedCustomer.status());

                return toResponse(savedCustomer);
        }

        /**
         * Deactivates a customer.
         */
        public CustomerResponse deactivateCustomer(Long customerId) {

                log.info("Execution step started: deactivateCustomer for customerId={}", customerId);

                Customer customer = customerRepository
                                .findById(CustomerId.of(customerId))
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Customer not found: " + customerId));

                customer.deactivate();

                Customer savedCustomer = customerRepository.save(customer);

                log.info(
                                "Execution step completed: deactivateCustomer for customerId={}, status={}",
                                customerId,
                                savedCustomer.status());

                return toResponse(savedCustomer);
        }

        /**
         * Closes a customer.
         *
         * Closing is a terminal lifecycle operation.
         */
        public CustomerResponse closeCustomer(Long customerId) {

                log.info("Execution step started: closeCustomer for customerId={}", customerId);

                Customer customer = customerRepository
                                .findById(CustomerId.of(customerId))
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Customer not found: " + customerId));

                customer.close();

                Customer savedCustomer = customerRepository.save(customer);

                log.info(
                                "Execution step completed: closeCustomer for customerId={}, status={}",
                                customerId,
                                savedCustomer.status());

                return toResponse(savedCustomer);
        }

}