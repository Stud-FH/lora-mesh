import { random as randomHex } from "../util/hex";
import { MeshNode } from "./node";

export type Method = 'adv' | 'seek' | 'send';

export class Message {
    conversationId = randomHex(6);
    method!: Method;
    originId!: string;
    senderId!: string;
    recipientId?: string;
    targetId?: string;
    content?: string;
}

export class Advertisement extends Message {
    constructor(public readonly sender: MeshNode, public readonly origin?: MeshNode) {
        super();
        this.method = 'adv';
        this.senderId = sender.id
    }
}