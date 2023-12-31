package com.user.service.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.user.service.entities.Hotel;
import com.user.service.entities.Rating;
import com.user.service.entities.User;
import com.user.service.exception.ResourceNotFoundException;
import com.user.service.externalServices.HotelService;
import com.user.service.externalServices.RatingService;
import com.user.service.repositories.UserRepository;
import com.user.service.services.UserService;

@Service
public class userServiceImpl implements UserService {
	
	
	@Autowired
	private UserRepository dbHandler;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private HotelService hotelService;
	
	@Autowired
	private RatingService ratingService;
	
	private Logger logger = LoggerFactory.getLogger(userServiceImpl.class);
	@Override
	public List<User> getAllUsers() {
		// TODO Auto-generated method stub
		List<User> users = dbHandler.findAll();
		// get ratings of all the users
		return users;
		
	}

	@Override
	@Cacheable("user")
	public User getUser(String id) {
		
		try {
			
			
		logger.info("calling database to get id: " + id);
		User user = dbHandler.findById(id).orElseThrow(()-> new ResourceNotFoundException("resource not present"));
		
		// get the ratings from rating microservice using httplicent(RestTemplate);
		// Rating[] ratings = restTemplate.getForObject("http://rating-service/Ratings/Users/"+id, Rating[].class);
		// List<Rating> userRatings = Arrays.asList(ratings);
		// user.setRating(ratings);
		 
		List<Rating> userRatings = ratingService.getRatings(id);
		 logger.info("ratings are :" + userRatings.toString());
		 
		List<Rating> finalRatingList = userRatings.stream().map(rating -> {
			//ResponseEntity<Hotel> hotelEntity = restTemplate.getForEntity("http://hotel-service/Hotels/"+rating.getHotelId(),Hotel.class);
			Hotel hotel = hotelService.getHotel(rating.getHotelId());
			rating.setHotel(hotel);
			return rating;
		 }).collect(Collectors.toList());
		user.setRating(finalRatingList);
		return user;
		}catch(Exception e) {
			throw new ResourceNotFoundException("resource not present");
		}
	}

	@Override
	public User saveUser(User user) {
		// TODO Auto-generated method stub
		String id = UUID.randomUUID().toString();
		user.setUserId(id);
		return dbHandler.save(user);
	}

	@Override
	public void deleteUser(String id)throws ResourceNotFoundException {
		// TODO Auto-generated method stub
		if(dbHandler.findById(id).isPresent()) {
		  dbHandler.deleteById(id);
		  return;
		 
		}
		throw new ResourceNotFoundException("user with given id doesn't present");
	}
	@Override
	public User updateUser(User user) throws ResourceNotFoundException {
		// TODO Auto-generated method stub
		Optional<User> optionalUser = dbHandler.findById(user.getUserId());
		if(optionalUser.isPresent()) {
			User usr = optionalUser.get();
			usr.setName(user.getName());
			usr.setAbout(user.getAbout());
			usr.setEmail(user.getEmail());
			return dbHandler.save(usr);
			
		}
		throw new ResourceNotFoundException("User with given id is not present");
		
		
	}

}
