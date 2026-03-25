package com.aditi.autify.service;

import com.aditi.autify.entity.UserEntity;
import com.aditi.autify.io.ProfileRequest;
import com.aditi.autify.io.ProfileResponse;
import com.aditi.autify.repository.UserRepostory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Service
public class ProfileServiceImpl implements ProfileService{

    private final UserRepostory userRepostory;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public ProfileResponse createProfile(ProfileRequest request){
        UserEntity newProfile = convertToUserEntity(request);
        if(!userRepostory.existsByEmail(request.getEmail())){
            newProfile = userRepostory.save(newProfile);
            return convertToProfileResponse(newProfile);
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    @Override
    public ProfileResponse getProfile(String email) {
        UserEntity existingUser = userRepostory.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: "+email));
        return convertToProfileResponse(existingUser);
    }

    @Override
    public void sendResetOtp(String email){
        UserEntity existingUser = userRepostory.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User not found: "+ email));

        //Generate 6 digit otp
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        //calculate expiry time(current time * 15 mins in milliseconds)

        long expiryTime = System.currentTimeMillis() + (15*60*1000);

        //update the profile/user
        existingUser.setResetOtp(otp);
        existingUser.setResetOtpExpireAt(expiryTime);

        //save into the database
        userRepostory.save(existingUser);

        try{

            emailService.sendResetOtpEmail(existingUser.getEmail(), otp);
        }catch(Exception ex){
            throw new RuntimeException("Unable to send email");
        }
    }

    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        UserEntity existingUser = userRepostory.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User not found: "+email));

        if(existingUser.getResetOtp() == null || !existingUser.getResetOtp().equals(otp)){
            throw new RuntimeException("Invalid OTP");
        }

        if(existingUser.getResetOtpExpireAt() < System.currentTimeMillis()){
            throw new RuntimeException("OTP Expired");
        }

        existingUser.setPassword(passwordEncoder.encode(newPassword));
        existingUser.setResetOtp(null);
        existingUser.setResetOtpExpireAt(0L);

        userRepostory.save(existingUser);
    }

    @Override
    public void sendOtp(String email) {
        System.out.println("📩 [DEBUG] Sending OTP to email: " + email);
        UserEntity exisitingUser = userRepostory.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        System.out.println("🔍 [DEBUG] Is Verified: " + exisitingUser.getIsAccountVerified());

        if (exisitingUser.getIsAccountVerified() != null && exisitingUser.getIsAccountVerified()) {
            System.out.println("✅ [DEBUG] User already verified. Skipping OTP send.");
            return;
        }

        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);

        exisitingUser.setVerifyOtp(otp);
        exisitingUser.setVerifyOtpExpireAt(expiryTime);
        userRepostory.save(exisitingUser);

        System.out.println("📨 [DEBUG] Calling EmailService.sendOtpEmail() with OTP: " + otp);

        try {
            emailService.sendOtpEmail(email, otp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to send email");
        }
    }


    @Override
    public void verifyOtp(String email, String otp) {
        UserEntity existingUser = userRepostory.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User not found"+email));

        if(existingUser.getVerifyOtp() == null || !existingUser.getVerifyOtp().equals(otp)){
            throw new RuntimeException("Invalid OTP");
        }

        if(existingUser.getVerifyOtpExpireAt() < System.currentTimeMillis()){
            throw new RuntimeException("OTP Expired");
        }

        existingUser.setIsAccountVerified(true);
        existingUser.setVerifyOtp(null);
        existingUser.setVerifyOtpExpireAt(0L);

        userRepostory.save(existingUser);

    }

    private UserEntity convertToUserEntity(ProfileRequest request){
        return UserEntity.builder()
                .email(request.getEmail())
                .userId(UUID.randomUUID().toString())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .isAccountVerified(false)
                .resetOtpExpireAt(0L)
                .verifyOtp(null)
                .verifyOtpExpireAt(0L)
                .resetOtp(null)
                .build();
    }

    private ProfileResponse convertToProfileResponse(UserEntity newProfile){
        return ProfileResponse.builder()
                .name(newProfile.getName())
                .email(newProfile.getEmail())
                .userId(newProfile.getUserId())
                .isAccountVerified(newProfile.getIsAccountVerified())
                .build();
    }


}
