export class MeshNode {
    aliases: string[]= [];
    route: {[key: string]: number} = {};

    constructor(public id: string) { }
}

export const self = new MeshNode('BIN');