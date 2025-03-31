package com.codingworld.service1.controller;

import com.codingworld.service1.blockchain.BlockchainService;
import com.codingworld.service1.utils.CryptoHelper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/")
public class Service1Controller {

    @PostMapping("login")
    public LoginResponse login(@RequestBody Login login){
        String email = login.getEmail();
        String password = login.getPassword();
        return new LoginResponse("Dnayeshwar",email,"/profilePic","hello","active",true);

    }


    @GetMapping("/friends")
    public ChatList getFriends() {
        List<String> list = Arrays.asList("John Doe", "Alice Smith", "Bob Johnson", "Emma Brown", "Charlie Davis","Dnyaneshwar Bhusare","John Doe", "Alice Smith", "Bob Johnson", "Emma Brown", "Charlie Davis","Dnyaneshwar Bhusare","John Doe", "Alice Smith", "Bob Johnson", "Emma Brown", "Charlie Davis","Dnyaneshwar Bhusare","John Doe", "Alice Smith", "Bob Johnson", "Emma Brown", "Charlie Davis","Dnyaneshwar Bhusare");
        ChatList chatList = new ChatList();
        chatList.setFriends(list);
        return chatList;
    }

    @PostMapping("/sendMessage")
    public String sendMessage(@RequestBody ChatMessage message) {
        String decrypt = CryptoHelper.decrypt(message.getMessage(), message.getAlgo());
        System.out.println("Received Message: " + message);

        System.out.println("Recrypted msg: " + decrypt);

        BlockchainService blockchain = new BlockchainService();

        try {
            String txHash = blockchain.storeMessageHash(
                    message.getMessage(),
                    "0x0fC5025C764cE34df352757e82f7B5c4Df39A836"
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            throw new RuntimeException(e);
        }


        return "Message sent successfully!";
    }

    @GetMapping("/getMessages")
    public List<GetMessages> getMesages(@RequestParam("from") String from) {
        System.out.println("getting all the messages from "+from);
        List<GetMessages> messages= new ArrayList<>();
        GetMessages messages1=new GetMessages();
        GetMessages messages2 = new GetMessages();
        messages1.setSent(true);
        messages1.setMessage("hi");
        messages1.setTs("yesterday");
        messages2.setSent(false);
        messages2.setMessage("hello");
        messages2.setTs("yesterday");
        messages.add(messages1);
        messages.add(messages2);
        return messages;
    }

}
